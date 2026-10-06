package com.aries.backend;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class AdminCatalogIntegrationTests extends IntegrationTestSupport {
    /** 同标题不产生地址冲突，重命名后仍按同一个 ID 读取；公开规则同样作用于 ID。 */
    @Test
    void idsRemainUniqueAndStableWhenTitlesRepeatOrChange() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        String body =
                """
                {"categoryId":1,"title":"同一个标题","summary":"地址由系统分配",
                "publicationType":"ARTICLE","accessType":"FREE","creditPrice":0,
                "previewMarkdown":"预览","fullMarkdown":"完整正文","requirements":"",
                "deliverables":"","version":"1.0"}
                """;
        var first =
                mvc.perform(
                                post("/api/v1/admin/publications")
                                        .cookie(admin)
                                        .contentType("application/json")
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andReturn();
        var second =
                mvc.perform(
                                post("/api/v1/admin/publications")
                                        .cookie(admin)
                                        .contentType("application/json")
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andReturn();
        String id =
                com.jayway.jsonpath.JsonPath.read(
                        first.getResponse().getContentAsString(), "$.data.id");
        String otherId =
                com.jayway.jsonpath.JsonPath.read(
                        second.getResponse().getContentAsString(), "$.data.id");
        assertThat(id).isNotEqualTo(otherId);
        mvc.perform(get("/api/v1/publications/" + id)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/publications/" + id + "/publish").cookie(admin))
                .andExpect(status().isOk());
        mvc.perform(
                        put("/api/v1/admin/publications/" + id)
                                .cookie(admin)
                                .contentType("application/json")
                                .content(body.replace("同一个标题", "修改后的标题")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/publications/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publication.id").value(id))
                .andExpect(jsonPath("$.data.publication.title").value("修改后的标题"));
        mvc.perform(get("/api/v1/publications/999999999999999999999999999999"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/publications/" + id + "/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markdown").value("完整正文"));
    }

    @Test
    void publishingWithoutFullContentReturnsConflictAndKeepsDraft() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        database.deletePublicationContent(13);

        mvc.perform(post("/api/v1/admin/publications/13/publish").cookie(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PUBLICATION_CONTENT_REQUIRED"));
        mvc.perform(get("/api/v1/publications/draft-case")).andExpect(status().isNotFound());
        assertThat(database.publicationStatus(13)).isEqualTo("DRAFT");
    }

    @Test
    void adminCanCreatePublishAndArchivePublications() throws Exception {
        mvc.perform(get("/api/v1/admin/publications")).andExpect(status().isUnauthorized());

        Cookie member = register("ordinary@example.com", "ordinary123", "普通成员");
        mvc.perform(get("/api/v1/admin/publications").cookie(member))
                .andExpect(status().isForbidden());

        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        mvc.perform(get("/api/v1/users/me").cookie(admin))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
        mvc.perform(get("/api/v1/admin/categories").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        var created =
                mvc.perform(
                                post("/api/v1/admin/publications")
                                        .cookie(admin)
                                        .contentType("application/json")
                                        .content(
                                                """
                                                {
                                                  "categoryId":1,
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
                        .andExpect(jsonPath("$.data.status").value("DRAFT"))
                        .andReturn();
        String id =
                com.jayway.jsonpath.JsonPath.read(
                        created.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(get("/api/v1/publications/" + id)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/publications/" + id + "/publish").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/publications/" + id)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/publications/" + id + "/archive").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        mvc.perform(get("/api/v1/publications/" + id)).andExpect(status().isNotFound());
    }
}
