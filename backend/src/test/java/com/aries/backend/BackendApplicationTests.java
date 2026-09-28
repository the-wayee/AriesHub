package com.aries.backend;

import com.aries.backend.catalog.infrastructure.persistence.mapper.CatalogReadMapper;
import com.aries.backend.identity.infrastructure.persistence.mapper.UserMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 使用真实 PostgreSQL 验证公开查询及敏感正文隔离，不用内存数据库代替方言测试。 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BackendApplicationTests {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogReadMapper mapper;
    @Autowired UserMapper userMapper;

    @BeforeEach
    void fixtures() {
        jdbc.execute("TRUNCATE case_contents, cases, categories, users RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO categories(id, slug, name) VALUES (1, 'coding', 'AI 编程'), (2, 'slides', 'AI 演示')");
        insert(11, 1, "free-case", "免费 AI 工具", "FREE", "PUBLISHED", "AVAILABLE", "FREE_BODY");
        insert(12, 2, "paid-case", "付费 PPT 案例", "PAID", "PUBLISHED", "AVAILABLE", "PAID_SECRET_SENTINEL");
        insert(13, 1, "draft-case", "草稿", "FREE", "DRAFT", "AVAILABLE", "DRAFT_SECRET_SENTINEL");
        insert(14, 1, "archived-case", "下架", "FREE", "ARCHIVED", "AVAILABLE", "ARCHIVED_SECRET_SENTINEL");
        insert(15, 1, "suspended-case", "停用", "FREE", "PUBLISHED", "SUSPENDED", "SUSPENDED_SECRET_SENTINEL");
    }

    private void insert(long id, int category, String slug, String title, String access,
                        String status, String delivery, String content) {
        jdbc.update("""
            INSERT INTO cases (id, category_id, slug, title, summary, access_type, price_minor,
                status, delivery_status, is_demo, published_at)
            VALUES (?, ?, ?, ?, '案例摘要', ?, ?, ?, ?, true, '2026-09-28T00:00:00Z')
            """, id, category, slug, title, access, "FREE".equals(access) ? 0 : 1990, status, delivery);
        jdbc.update("""
            INSERT INTO case_contents(case_id, preview_markdown, full_markdown, requirements, deliverables)
            VALUES (?, '公开预览', ?, '基础要求', '交付清单')
            """, id, content);
    }

    @Test void healthChecksActualDatabase() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.database").value("UP"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class)).isPositive();
    }

    @Test void listAndCountsOnlyIncludePublishedAvailableCases() throws Exception {
        mvc.perform(get("/api/v1/cases")).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(content().string(not(containsString("SECRET_SENTINEL"))))
            .andExpect(content().string(not(containsString("fullMarkdown"))))
            .andExpect(content().string(not(containsString("previewMarkdown"))))
            .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/v1/categories")).andExpect(jsonPath("$[0].caseCount").value(1))
            .andExpect(jsonPath("$[1].caseCount").value(1));
    }

    @Test void categoryKeywordAccessAndPaginationWorkTogether() throws Exception {
        mvc.perform(get("/api/v1/cases").param("category", "coding").param("q", "AI").param("access", "FREE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value("11"));
        mvc.perform(get("/api/v1/cases").param("size", "1").param("page", "2"))
            .andExpect(jsonPath("$.items[0].slug").value("free-case"))
            .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/cases").param("page", "99"))
            .andExpect(jsonPath("$.items", hasSize(0))).andExpect(jsonPath("$.total").value(2));
    }

    @Test void searchTreatsWildcardsAndSqlAsLiteralText() throws Exception {
        for (String keyword : new String[] {"%", "_", "' OR 1=1 --", "PAID_SECRET_SENTINEL"}) {
            mvc.perform(get("/api/v1/cases").param("q", keyword))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        }
    }

    @Test void paidDetailsExposePreviewButNeverBody() throws Exception {
        mvc.perform(get("/api/v1/cases/paid-case")).andExpect(status().isOk())
            .andExpect(jsonPath("$.preview.previewMarkdown").value("公开预览"))
            .andExpect(content().string(not(containsString("PAID_SECRET_SENTINEL"))));
        mvc.perform(get("/api/v1/cases/12/content")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CONTENT_LOCKED"))
            .andExpect(content().string(not(containsString("PAID_SECRET_SENTINEL"))));
        assertThat(mapper.freeContent(12)).isNull();
    }

    @Test void onlyFreePublishedContentIsReadable() throws Exception {
        mvc.perform(get("/api/v1/cases/11/content")).andExpect(status().isOk())
            .andExpect(jsonPath("$.markdown").value("FREE_BODY"));
        for (String slug : new String[]{"draft-case", "archived-case", "suspended-case", "missing"}) {
            mvc.perform(get("/api/v1/cases/" + slug)).andExpect(status().isNotFound());
        }
        for (int id : new int[]{13, 14, 15, 999}) {
            mvc.perform(get("/api/v1/cases/" + id + "/content")).andExpect(status().isNotFound());
            assertThat(mapper.freeContent(id)).isNull();
        }
    }

    @Test void invalidInputsReturnSafeStructuredErrors() throws Exception {
        for (String[] entry : new String[][]{{"page", "0"}, {"page", "10001"}, {"size", "25"},
                {"size", "0"}, {"page", "abc"}, {"access", "INVALID"}, {"category", "../x"}, {"q", "x".repeat(121)}}) {
            var result = mvc.perform(get("/api/v1/cases").param(entry[0], entry[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist()).andReturn();
            assertThat(result.getResponse().getContentAsString())
                .contains(result.getResponse().getHeader("X-Request-Id"));
        }
        mvc.perform(get("/api/v1/cases/-1/content")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/cases/abc/content")).andExpect(status().isBadRequest());
    }

    @Test void unpublishingImmediatelyHidesDetailAndBody() throws Exception {
        jdbc.update("UPDATE cases SET status = 'ARCHIVED' WHERE id = 11");
        mvc.perform(get("/api/v1/cases/free-case")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/cases/11/content")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/cases").param("access", "FREE")).andExpect(jsonPath("$.total").value(0));
    }

    @Test void bigintIdsAreReturnedAsStrings() throws Exception {
        insert(9007199254740993L, 1, "large-id", "大 ID", "FREE", "PUBLISHED", "AVAILABLE", "body");
        mvc.perform(get("/api/v1/cases/large-id"))
            .andExpect(jsonPath("$.caseInfo.id").value("9007199254740993"));
    }

    @Test void databaseRejectsInvalidPricing() {
        assertThatThrownBy(() -> jdbc.update("UPDATE cases SET price_minor = -1 WHERE id = 12"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE cases SET price_minor = 50 WHERE id = 11"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void unknownEndpointsAndUnsupportedMethodsAreJsonErrors() throws Exception {
        mvc.perform(get("/api/v1/unknown")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(post("/api/v1/cases")).andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test void registrationCreatesSessionAndStoresPasswordHash() throws Exception {
        var registered = mvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                            {"email":"Hello@Example.com","password":"hello1234","nickname":"小羊"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.email").value("hello@example.com"))
                .andExpect(jsonPath("$.nickname").value("小羊"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andReturn();

        String passwordHash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE email = 'hello@example.com'", String.class);
        assertThat(passwordHash).startsWith("$2").doesNotContain("hello1234");
        assertThat(jdbc.queryForObject(
                "SELECT created_at IS NOT NULL AND updated_at IS NOT NULL AND NOT is_deleted FROM users WHERE id = 1",
                Boolean.class)).isTrue();

        Cookie session = registered.getResponse().getCookie("arieshub_token");
        assertThat(session).isNotNull();
        mvc.perform(get("/api/v1/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("hello@example.com"));
    }

    @Test void duplicateRegistrationAndInvalidLoginReturnStableErrors() throws Exception {
        String body = """
                {"email":"member@example.com","password":"member123","nickname":"成员"}
                """;
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("""
                        {"email":"member@example.com","password":"wrong-password"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test void loginAndLogoutCompleteTheCookieSessionLifecycle() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                        {"email":"login@example.com","password":"login1234","nickname":"登录用户"}
                        """))
                .andExpect(status().isCreated());

        var loggedIn = mvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"email":"LOGIN@example.com","password":"login1234"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andReturn();
        Cookie session = loggedIn.getResponse().getCookie("arieshub_token");
        assertThat(session).isNotNull();
        assertThat(jdbc.queryForObject(
                "SELECT last_login_at IS NOT NULL FROM users WHERE email = 'login@example.com'", Boolean.class))
                .isTrue();

        mvc.perform(post("/api/v1/auth/logout").cookie(session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").cookie(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test void validationAndLogicalDeleteAreAppliedByFramework() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                        {"email":"bad","password":"short","nickname":"x"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                        {"email":"delete@example.com","password":"delete1234","nickname":"待删除"}
                        """))
                .andExpect(status().isCreated());
        userMapper.deleteById(1L);
        assertThat(userMapper.selectById(1L)).isNull();
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM users WHERE id = 1", Boolean.class)).isTrue();
    }

    @Test void protectedIdentityEndpointsRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
