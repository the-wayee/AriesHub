package com.aries.backend;

import jakarta.servlet.http.Cookie;
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
class AdminCatalogIntegrationTests extends IntegrationTestSupport {
    @Test void publishingWithoutFullContentReturnsConflictAndKeepsDraft() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        database.deletePublicationContent(13);

        mvc.perform(post("/api/v1/admin/publications/13/publish").cookie(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PUBLICATION_CONTENT_REQUIRED"));
        mvc.perform(get("/api/v1/publications/draft-case"))
                .andExpect(status().isNotFound());
        assertThat(database.publicationStatus(13)).isEqualTo("DRAFT");
    }

    @Test void adminCanCreatePublishAndArchivePublications() throws Exception {
        mvc.perform(get("/api/v1/admin/publications"))
                .andExpect(status().isUnauthorized());

        Cookie member = register("ordinary@example.com", "ordinary123", "普通成员");
        mvc.perform(get("/api/v1/admin/publications").cookie(member))
                .andExpect(status().isForbidden());

        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        mvc.perform(get("/api/v1/users/me").cookie(admin))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/v1/admin/categories").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(post("/api/v1/admin/publications").cookie(admin)
                        .contentType("application/json")
                        .content("""
                            {
                              "categoryId":1,
                              "slug":"admin-created-case",
                              "title":"后台创建的案例",
                              "summary":"验证管理员可以维护内容",
                              "publicationType":"CASE_STUDY",
                              "accessType":"FREE",
                              "creditPrice":0,
                              "previewMarkdown":"## 公开预览",
                              "fullMarkdown":"## 完整正文",
                              "requirements":"准备条件",
                              "deliverables":"交付说明",
                              "version":"1.0"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
        Long id = database.publicationIdBySlug("admin-created-case");

        mvc.perform(get("/api/v1/publications/admin-created-case"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/publications/" + id + "/publish").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/publications/admin-created-case"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/publications/" + id + "/archive").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mvc.perform(get("/api/v1/publications/admin-created-case"))
                .andExpect(status().isNotFound());
    }
}
