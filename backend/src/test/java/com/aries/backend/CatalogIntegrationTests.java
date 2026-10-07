package com.aries.backend;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class CatalogIntegrationTests extends IntegrationTestSupport {
    private Cookie reader;

    @BeforeEach
    void readerSession() throws Exception {
        reader = register("catalog-reader@example.com", "reader1234", "读者");
    }

    @Test
    void listAndCountsOnlyIncludePublishedAvailablePublications() throws Exception {
        mvc.perform(get("/api/v1/publications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(content().string(not(containsString("SECRET_SENTINEL"))))
                .andExpect(content().string(not(containsString("fullMarkdown"))))
                .andExpect(content().string(not(containsString("previewMarkdown"))))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$.data[0].publicationCount").value(1))
                .andExpect(jsonPath("$.data[1].publicationCount").value(1));
    }

    @Test
    void categoryKeywordAccessAndPaginationWorkTogether() throws Exception {
        mvc.perform(
                        get("/api/v1/publications")
                                .param("category", "coding")
                                .param("q", "AI")
                                .param("type", "CASE_STUDY")
                                .param("access", "FREE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("11"));
        mvc.perform(get("/api/v1/publications").param("size", "1").param("page", "2"))
                .andExpect(jsonPath("$.data.items[0].id").value("11"))
                .andExpect(jsonPath("$.data.totalPages").value(2));
        mvc.perform(get("/api/v1/publications").param("page", "99"))
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void searchTreatsWildcardsAndSqlAsLiteralText() throws Exception {
        for (String keyword : new String[] {"%", "_", "' OR 1=1 --", "CREDIT_SECRET_SENTINEL"}) {
            mvc.perform(get("/api/v1/publications").param("q", keyword))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0));
        }
    }

    @Test
    void creditDetailsExposePreviewButNeverBody() throws Exception {
        mvc.perform(get("/api/v1/publications/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preview.previewMarkdown").value("公开预览"))
                .andExpect(jsonPath("$.data.publication.creditPrice").value(199))
                .andExpect(content().string(not(containsString("CREDIT_SECRET_SENTINEL"))));
        mvc.perform(get("/api/v1/publications/12/content").cookie(reader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONTENT_LOCKED"))
                .andExpect(content().string(not(containsString("CREDIT_SECRET_SENTINEL"))));
        assertThat(mapper.freeContent(12)).isNull();
    }

    @Test
    void onlyFreePublishedContentIsReadable() throws Exception {
        mvc.perform(get("/api/v1/publications/11/content").cookie(reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markdown").value("FREE_BODY"));
        for (String id : new String[] {"13", "14", "15", "999"}) {
            mvc.perform(get("/api/v1/publications/" + id)).andExpect(status().isNotFound());
        }
        for (int id : new int[] {13, 14, 15, 999}) {
            mvc.perform(get("/api/v1/publications/" + id + "/content").cookie(reader))
                    .andExpect(status().isNotFound());
            assertThat(mapper.freeContent(id)).isNull();
        }
    }

    /** ID 详情与评论统一挂载；旧名称、溢出及非正数不能触发查询回退。 */
    @Test
    void detailRejectsNameAddressesAndInvalidIds() throws Exception {
        for (String id : new String[] {"free-case", "0", "-1", "9223372036854775808"}) {
            mvc.perform(get("/api/v1/publications/" + id))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        mvc.perform(get("/api/v1/publications/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publication.id").value("11"))
                .andExpect(jsonPath("$.data.publication.slug").doesNotExist());
    }

    @Test
    void invalidInputsReturnSafeStructuredErrors() throws Exception {
        for (String[] entry :
                new String[][] {
                    {"page", "0"},
                    {"page", "10001"},
                    {"size", "25"},
                    {"size", "0"},
                    {"page", "abc"},
                    {"access", "INVALID"},
                    {"category", "../x"},
                    {"q", "x".repeat(121)}
                }) {
            MvcResult result =
                    mvc.perform(get("/api/v1/publications").param(entry[0], entry[1]))
                            .andExpect(status().isBadRequest())
                            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                            .andExpect(jsonPath("$.traceId").isNotEmpty())
                            .andExpect(jsonPath("$.trace").doesNotExist())
                            .andReturn();
            assertThat(result.getResponse().getContentAsString())
                    .contains(result.getResponse().getHeader("X-Request-Id"));
        }
        mvc.perform(get("/api/v1/publications/-1/content").cookie(reader))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/publications/abc/content").cookie(reader))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unpublishingImmediatelyHidesDetailAndBody() throws Exception {
        database.updatePublicationStatus(11, "ARCHIVED");
        mvc.perform(get("/api/v1/publications/11")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/publications/11/content").cookie(reader))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/publications").param("access", "FREE"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void bigintIdsAreReturnedAsStrings() throws Exception {
        insert(9007199254740993L, 1, "大 ID", "FREE", "PUBLISHED", "AVAILABLE", "body");
        mvc.perform(get("/api/v1/publications/9007199254740993"))
                .andExpect(jsonPath("$.data.publication.id").value("9007199254740993"));
    }

    @Test
    void databaseRejectsInvalidCreditPricingAndAccessTypes() {
        // CREDIT 内容不能是 0 积分：约束已从 Java 的 Publication.validate() 下沉到数据库。
        assertThatThrownBy(() -> database.updatePublicationCreditPrice(12, 0))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // FREE 内容不能带价格。
        assertThatThrownBy(() -> database.updatePublicationCreditPrice(11, 10))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> database.updatePublicationAccessType(11, "PAID"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void unknownEndpointsAndUnsupportedMethodsAreJsonErrors() throws Exception {
        mvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(post("/api/v1/publications"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
