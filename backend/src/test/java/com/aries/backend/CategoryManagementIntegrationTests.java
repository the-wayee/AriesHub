package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** 验证分类从管理员创建到文章归类及公开筛选的完整链路。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class CategoryManagementIntegrationTests extends IntegrationTestSupport {
    @Test
    void updatesPreserveIdentityAndDeletionProtectsReferencedContent() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        mvc.perform(
                        put("/api/v1/admin/categories/1")
                                .cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":" 创意编程 ","color":"#aa6633"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("创意编程"))
                .andExpect(jsonPath("$.data.color").value("#AA6633"))
                .andExpect(jsonPath("$.data.slug").value("coding"));
        mvc.perform(
                        put("/api/v1/admin/categories/2")
                                .cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"创意编程","color":"#AA6633"}
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_CONFLICT"));
        mvc.perform(delete("/api/v1/admin/categories/1").cookie(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"));
        mvc.perform(get("/api/v1/categories")).andExpect(jsonPath("$.data[0].name").value("创意编程"));
        mvc.perform(
                        post("/api/v1/admin/categories")
                                .cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"空分类\"}"))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/v1/admin/categories/3").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        mvc.perform(delete("/api/v1/admin/categories/3").cookie(admin))
                .andExpect(status().isNotFound());
        mvc.perform(
                        put("/api/v1/admin/categories/3")
                                .cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"已删除","color":"#AA6633"}
                                        """))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/categories").cookie(admin))
                .andExpect(jsonPath("$.data.length()").value(2));
        // 逻辑删除释放名称，后续创建获得新 ID，不能复活旧引用。
        mvc.perform(
                        post("/api/v1/admin/categories")
                                .cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"空分类\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("4"));
    }

    @Test
    void categoryMutationsRequireAdminAndValidUpdate() throws Exception {
        String body = "{\"name\":\"视频\",\"color\":\"#336699\"}";
        mvc.perform(
                        put("/api/v1/admin/categories/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/admin/categories/1")).andExpect(status().isUnauthorized());
        Cookie member = register("member@example.com", "member1234", "成员");
        mvc.perform(
                        put("/api/v1/admin/categories/1")
                                .cookie(member)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/categories/1").cookie(member))
                .andExpect(status().isForbidden());
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        for (String invalid :
                new String[] {
                    """
                    {"name":" ","color":"#336699"}
                    """,
                    """
                    {"name":"视频","color":"red"}
                    """
                }) {
            mvc.perform(
                            put("/api/v1/admin/categories/1")
                                    .cookie(admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(invalid))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void colorsAndOrderingArePersistedAndInvalidListsCannotPartiallyUpdate() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        mvc.perform(
                        post("/api/v1/admin/categories")
                                .cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"彩色分类","color":"#aabbcc","sortOrder":9}
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.color").value("#AABBCC"));
        mvc.perform(
                        put("/api/v1/admin/categories/order")
                                .cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"categoryIds":["3","2","1"]}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("3"))
                .andExpect(jsonPath("$.data[0].sortOrder").value(0));
        for (String ids : new String[] {"[1,1,3]", "[1,2]", "[1,2,999]"}) {
            mvc.perform(
                            put("/api/v1/admin/categories/order")
                                    .cookie(admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"categoryIds\":" + ids + "}"))
                    .andExpect(status().isConflict());
        }
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("3"))
                .andExpect(jsonPath("$.data[0].color").value("#AABBCC"));
        mvc.perform(
                        post("/api/v1/admin/categories")
                                .cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"非法颜色","color":"red"}
                                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/api/v1/admin/categories/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"categoryIds\":[1,2,3]}"))
                .andExpect(status().isUnauthorized());
        Cookie member = register("member@example.com", "member1234", "成员");
        mvc.perform(
                        put("/api/v1/admin/categories/order")
                                .cookie(member)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"categoryIds\":[1,2,3]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createdCategoryCanBeSelectedAndPublished() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        MvcResult result =
                mvc.perform(
                                post("/api/v1/admin/categories")
                                        .cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\" AI 视频创作 \",\"sortOrder\":5}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.name").value("AI 视频创作"))
                        .andExpect(jsonPath("$.traceId").isNotEmpty())
                        .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        String slug = JsonPath.read(result.getResponse().getContentAsString(), "$.data.slug");
        assertThat(slug).startsWith("category-");
        mvc.perform(get("/api/v1/admin/categories").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(id)));
        String publication =
                """
                {"categoryId":%s,"title":"视频分类里的文章","summary":"分类联调",
                 "publicationType":"ARTICLE","accessType":"FREE","creditPrice":0,
                 "previewMarkdown":"预览","fullMarkdown":"完整正文","requirements":"",
                 "deliverables":"","version":"1.0"}
                """
                        .formatted(id);
        MvcResult created =
                mvc.perform(
                                post("/api/v1/admin/publications")
                                        .cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(publication))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.categoryId").value(id))
                        .andReturn();
        String publicationId =
                JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
        mvc.perform(post("/api/v1/admin/publications/" + publicationId + "/publish").cookie(admin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].slug", hasItem(slug)));
        mvc.perform(get("/api/v1/publications/cards").param("category", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].publication.id").value(publicationId));
        mvc.perform(get("/api/v1/home").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[*].slug", hasItem(slug)));
        mvc.perform(
                        post("/api/v1/admin/categories")
                                .cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"AI 视频创作\",\"sortOrder\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_CONFLICT"));
    }

    @Test
    void categoryCreationRequiresAdminAndValidFields() throws Exception {
        mvc.perform(
                        post("/api/v1/admin/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"视频\"}"))
                .andExpect(status().isUnauthorized());
        Cookie member = register("member@example.com", "member1234", "成员");
        mvc.perform(
                        post("/api/v1/admin/categories")
                                .cookie(member)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"视频\"}"))
                .andExpect(status().isForbidden());
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        for (String body :
                new String[] {
                    "{\"name\":\"   \",\"sortOrder\":0}", "{\"name\":\"视频\",\"sortOrder\":-1}"
                }) {
            mvc.perform(
                            post("/api/v1/admin/categories")
                                    .cookie(admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
