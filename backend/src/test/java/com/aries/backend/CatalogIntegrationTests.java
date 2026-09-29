package com.aries.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class CatalogIntegrationTests extends IntegrationTestSupport {
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
}
