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
        jdbc.update("DELETE FROM case_contents WHERE case_id = 13");

        mvc.perform(post("/api/v1/admin/cases/13/publish").cookie(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CASE_CONTENT_REQUIRED"));
        mvc.perform(get("/api/v1/cases/draft-case"))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT status FROM cases WHERE id = 13", String.class))
                .isEqualTo("DRAFT");
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
}
