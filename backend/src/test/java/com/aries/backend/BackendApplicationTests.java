package com.aries.backend;

import com.aries.backend.catalog.infrastructure.persistence.mapper.CatalogReadMapper;
import com.aries.backend.identity.application.port.EmailCodeSender;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import com.aries.backend.identity.infrastructure.persistence.mapper.UserMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 使用真实 PostgreSQL 验证公开查询及敏感正文隔离，不用内存数据库代替方言测试。 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(BackendApplicationTests.EmailTestConfiguration.class)
class BackendApplicationTests {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("app.security.admin-emails", () -> "admin@example.com");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogReadMapper mapper;
    @Autowired UserMapper userMapper;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired FakeEmailCodeSender emailSender;

    @BeforeEach
    void fixtures() {
        jdbc.execute("TRUNCATE case_contents, cases, categories, users RESTART IDENTITY CASCADE");
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        emailSender.clear();
        jdbc.update("INSERT INTO categories(id, slug, name) VALUES (1, 'coding', 'AI 编程'), (2, 'slides', 'AI 演示')");
        insert(11, 1, "free-case", "免费 AI 工具", "FREE", "PUBLISHED", "AVAILABLE", "FREE_BODY");
        insert(12, 2, "paid-case", "付费 PPT 案例", "PAID", "PUBLISHED", "AVAILABLE", "PAID_SECRET_SENTINEL");
        insert(13, 1, "draft-case", "草稿", "FREE", "DRAFT", "AVAILABLE", "DRAFT_SECRET_SENTINEL");
        insert(14, 1, "archived-case", "下架", "FREE", "ARCHIVED", "AVAILABLE", "ARCHIVED_SECRET_SENTINEL");
        insert(15, 1, "suspended-case", "停用", "FREE", "PUBLISHED", "SUSPENDED", "SUSPENDED_SECRET_SENTINEL");
    }

    private String requestCode(String email, VerificationPurpose purpose) throws Exception {
        mvc.perform(post("/api/v1/auth/email-codes")
                        .contentType("application/json")
                        .content("""
                            {"email":"%s","purpose":"%s"}
                            """.formatted(email, purpose.name())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.expiresInSeconds").value(600))
                .andExpect(jsonPath("$.resendAfterSeconds").value(60));
        return emailSender.latest(email.toLowerCase(), purpose);
    }

    private Cookie register(String email, String password, String nickname) throws Exception {
        String code = requestCode(email, VerificationPurpose.REGISTER);
        var response = mvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                            {"email":"%s","password":"%s","nickname":"%s","code":"%s"}
                            """.formatted(email, password, nickname, code)))
                .andExpect(status().isCreated())
                .andReturn();
        return response.getResponse().getCookie("arieshub_token");
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
        String code = requestCode("Hello@Example.com", VerificationPurpose.REGISTER);
        var registered = mvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                            {"email":"Hello@Example.com","password":"hello1234","nickname":"小羊","code":"%s"}
                            """.formatted(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.email").value("hello@example.com"))
                .andExpect(jsonPath("$.nickname").value("小羊"))
                .andExpect(jsonPath("$.emailVerified").value(true))
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
        String code = requestCode("member@example.com", VerificationPurpose.REGISTER);
        String body = """
                {"email":"member@example.com","password":"member123","nickname":"成员","code":"%s"}
                """.formatted(code);
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("""
                        {"email":"member@example.com","password":"wrong-password","code":"000000"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test void loginAndLogoutCompleteTheCookieSessionLifecycle() throws Exception {
        register("login@example.com", "login1234", "登录用户");
        String loginCode = requestCode("login@example.com", VerificationPurpose.LOGIN);

        var loggedIn = mvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"email":"LOGIN@example.com","password":"login1234","code":"%s"}
                            """.formatted(loginCode)))
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
                        {"email":"bad","password":"short","nickname":"x","code":"12"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        register("delete@example.com", "delete1234", "待删除");
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

    @Test void verificationCodesAreHashedRateLimitedAndSingleUse() throws Exception {
        String email = "code@example.com";
        String code = requestCode(email, VerificationPurpose.REGISTER);
        mvc.perform(post("/api/v1/auth/email-codes").contentType("application/json").content("""
                        {"email":"code@example.com","purpose":"REGISTER"}
                        """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_TOO_FREQUENT"));

        var keys = redisTemplate.keys("arieshub:auth:code:register:*");
        assertThat(keys).hasSize(1).allMatch(key -> !key.contains(email));
        Object storedHash = redisTemplate.opsForHash().get(keys.iterator().next(), "hash");
        assertThat(storedHash).isNotNull().asString().startsWith("$2").doesNotContain(code);

        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                        {"email":"code@example.com","password":"code12345","nickname":"验证码用户","code":"000000"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_INVALID"));
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                        {"email":"code@example.com","password":"code12345","nickname":"验证码用户","code":"%s"}
                        """.formatted(code)))
                .andExpect(status().isCreated());
        assertThat(redisTemplate.keys("arieshub:auth:code:register:*")).isEmpty();
    }

    @Test void adminCanCreatePublishAndArchiveCases() throws Exception {
        mvc.perform(get("/api/v1/admin/cases"))
                .andExpect(status().isUnauthorized());

        Cookie member = register("ordinary@example.com", "ordinary123", "普通成员");
        mvc.perform(get("/api/v1/admin/cases").cookie(member))
                .andExpect(status().isForbidden());

        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        mvc.perform(get("/api/v1/auth/me").cookie(admin))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/v1/admin/categories").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(post("/api/v1/admin/cases").cookie(admin)
                        .contentType("application/json")
                        .content("""
                            {
                              "categoryId":1,
                              "slug":"admin-created-case",
                              "title":"后台创建的案例",
                              "summary":"验证管理员可以维护内容",
                              "accessType":"FREE",
                              "priceMinor":0,
                              "previewMarkdown":"## 公开预览",
                              "fullMarkdown":"## 完整正文",
                              "requirements":"准备条件",
                              "deliverables":"交付说明",
                              "version":"1.0"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
        Long id = jdbc.queryForObject("SELECT id FROM cases WHERE slug = 'admin-created-case'", Long.class);

        mvc.perform(get("/api/v1/cases/admin-created-case"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/cases/" + id + "/publish").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/cases/admin-created-case"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/cases/" + id + "/archive").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mvc.perform(get("/api/v1/cases/admin-created-case"))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EmailTestConfiguration {
        @Bean
        @Primary
        FakeEmailCodeSender fakeEmailCodeSender() {
            return new FakeEmailCodeSender();
        }
    }

    static class FakeEmailCodeSender implements EmailCodeSender {
        private final Map<String, String> codes = new ConcurrentHashMap<>();

        @Override
        public void send(String email, String code, VerificationPurpose purpose, String idempotencyKey) {
            codes.put(key(email, purpose), code);
        }

        String latest(String email, VerificationPurpose purpose) {
            return codes.get(key(email, purpose));
        }

        void clear() {
            codes.clear();
        }

        private String key(String email, VerificationPurpose purpose) {
            return purpose.name() + ":" + email;
        }
    }
}
