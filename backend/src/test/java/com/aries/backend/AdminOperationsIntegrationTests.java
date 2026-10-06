package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class AdminOperationsIntegrationTests extends IntegrationTestSupport {
    @org.springframework.beans.factory.annotation.Autowired
    com.aries.backend.catalog.application.port.PublicationUploadProgress uploadProgress;

    @Test
    void ossProgressIsOwnerScopedCannotBeReusedAndCancellationStopsUpdates() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        java.util.UUID id = java.util.UUID.randomUUID();
        String profile =
                mvc.perform(get("/api/v1/users/me").cookie(admin))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long owner =
                Long.parseLong(
                        com.jayway.jsonpath.JsonPath.parse(profile)
                                .read("$.data.id", String.class));
        uploadProgress.begin(owner, id, 20_000_000);
        uploadProgress.confirmed(owner, id, 5_000_000, 20_000_000);
        mvc.perform(get("/api/v1/admin/media/uploads/" + id).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.phase").value("OSS"))
                .andExpect(jsonPath("$.data.loaded").value(5_000_000));
        assertThat(uploadProgress.status(owner + 1, id).phase()).isEqualTo("RECEIVING");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> uploadProgress.begin(owner, id, 20_000_000))
                .isInstanceOf(
                        com.aries.backend.shared.application.exception.BusinessException.class);
        mvc.perform(delete("/api/v1/admin/media/uploads/" + id).cookie(admin))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> uploadProgress.confirmed(owner, id, 10_000_000, 20_000_000))
                .isInstanceOf(
                        com.aries.backend.shared.application.exception.BusinessException.class);
        uploadProgress.failed(owner, id);
        assertThat(uploadProgress.status(owner, id).phase()).isEqualTo("CANCELLED");
        mvc.perform(get("/api/v1/admin/media/uploads/" + id)).andExpect(status().isUnauthorized());
    }

    /** 验证真实数据库分页、转义与逻辑删除，并确保管理响应不返回密码摘要。 */
    @Test
    void membersUseDatabasePaginationAndLiteralCaseInsensitiveSearch() throws Exception {
        register("alpha@example.com", "member1234", "AlphaDev");
        register("special@example.com", "member1234", "AI_%!作者");
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        var first =
                mvc.perform(get("/api/v1/admin/users").cookie(admin).param("size", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.total").value(3))
                        .andExpect(jsonPath("$.data.items", hasSize(1)))
                        .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var second =
                mvc.perform(
                                get("/api/v1/admin/users")
                                        .cookie(admin)
                                        .param("size", "1")
                                        .param("page", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.items", hasSize(1)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(first.split("\"id\":\"")[1].split("\"")[0])
                .isNotEqualTo(second.split("\"id\":\"")[1].split("\"")[0]);
        for (String query : new String[] {"%", "_", "!"}) {
            mvc.perform(get("/api/v1/admin/users").cookie(admin).param("q", query))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.items[0].nickname").value("AI_%!作者"));
        }
        mvc.perform(get("/api/v1/admin/users").cookie(admin).param("q", "alphadev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        userMapper.deleteById(database.userIdByEmail("alpha@example.com"));
        mvc.perform(get("/api/v1/admin/users").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void operationsRequireAdminAndCountRealRecords() throws Exception {
        for (String path :
                new String[] {
                    "users", "operations/overview", "operations/ledger", "discussions/comments"
                }) mvc.perform(get("/api/v1/admin/" + path)).andExpect(status().isUnauthorized());
        Cookie member = register("member@example.com", "member1234", "实践成员");
        mvc.perform(get("/api/v1/admin/operations/overview").cookie(member))
                .andExpect(status().isForbidden());
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        database.insertThread("PUBLICATION", "free-case", "OPEN");
        database.insertRootComment(
                database.threadId("PUBLICATION", "free-case"),
                database.userIdByEmail("member@example.com"),
                "真实评论",
                0);
        mvc.perform(get("/api/v1/admin/operations/overview").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.members").value(2))
                .andExpect(jsonPath("$.data.summary.comments").value(1))
                .andExpect(jsonPath("$.data.summary.unlocks").value(0))
                .andExpect(jsonPath("$.data.days", hasSize(7)));
        mvc.perform(get("/api/v1/admin/operations/ledger").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/v1/admin/discussions/comments").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].body").value("真实评论"));
    }

    @Test
    void disablingMemberRevokesExistingSessionAndProtectsAdmins() throws Exception {
        Cookie member = register("member@example.com", "member1234", "实践成员");
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        long id = database.userIdByEmail("member@example.com");
        String password = database.passwordHash("member@example.com");
        mvc.perform(get("/api/v1/admin/users").param("q", "%").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/v1/admin/users").param("q", "实践").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(Long.toString(id)));
        mvc.perform(
                        put("/api/v1/admin/users/" + id + "/status")
                                .cookie(admin)
                                .contentType("application/json")
                                .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
        mvc.perform(get("/api/v1/users/me").cookie(member)).andExpect(status().isUnauthorized());
        assertThat(database.passwordHash("member@example.com")).isEqualTo(password);
        mvc.perform(
                        put("/api/v1/admin/users/"
                                        + database.userIdByEmail("admin@example.com")
                                        + "/status")
                                .cookie(admin)
                                .contentType("application/json")
                                .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/v1/admin/users/" + id + "/status")
                                .cookie(admin)
                                .contentType("application/json")
                                .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void draftSupportsEmptyBodyAndPaidMediaNeverLeaks() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        when(objects.imageUrl(anyString(), any()))
                .thenReturn("https://files.example.com/private-image.png");
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0};
        String cover = upload(admin, "COVER", "cover.png", "image/png", png);
        String body = upload(admin, "IMAGE", "body.png", "image/png", png);
        var created =
                mvc.perform(
                                post("/api/v1/admin/publications")
                                        .cookie(admin)
                                        .contentType("application/json")
                                        .content(draft("", cover)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String id =
                com.jayway.jsonpath.JsonPath.read(
                        created.getResponse().getContentAsString(), "$.data.id");
        mvc.perform(get("/api/v1/publications/" + id + "/media/" + cover + "/url"))
                .andExpect(status().isNotFound());
        mvc.perform(
                        put("/api/v1/admin/publications/" + id)
                                .cookie(admin)
                                .contentType("application/json")
                                .content(draft("![正文](media:" + body + ")", cover)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverFileId").value(cover));
        mvc.perform(post("/api/v1/admin/publications/" + id + "/publish").cookie(admin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/publications/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publication.featured").value(true));
        mvc.perform(get("/api/v1/publications/" + id + "/media/" + cover + "/url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").exists());
        mvc.perform(get("/api/v1/publications/" + id + "/media/" + body + "/url"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONTENT_LOCKED"));
        mvc.perform(
                        put("/api/v1/admin/publications/" + id)
                                .cookie(admin)
                                .contentType("application/json")
                                .content(draft("正文", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverFileId").doesNotExist());
        mvc.perform(get("/api/v1/publications/" + id + "/media/" + cover + "/url"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsSpoofedMediaAndUnknownAssetReferences() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        mvc.perform(
                        multipart("/api/v1/admin/media")
                                .cookie(admin)
                                .param("kind", "IMAGE")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "fake.png",
                                                "image/png",
                                                "not an image".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEDIA"));
        mvc.perform(
                        post("/api/v1/admin/publications")
                                .cookie(admin)
                                .contentType("application/json")
                                .content(draft("正文", java.util.UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        assertThat(database.publicationIdBySlug("media-course")).isNull();
    }

    private String upload(Cookie admin, String kind, String name, String type, byte[] content)
            throws Exception {
        String json =
                mvc.perform(
                                multipart("/api/v1/admin/media")
                                        .cookie(admin)
                                        .param("kind", kind)
                                        .file(new MockMultipartFile("file", name, type, content)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    private String draft(String markdown, String cover) {
        return """
        {"categoryId":1,"title":"AI 实践课程","summary":"学习工作流",
        "publicationType":"COURSE","accessType":"CREDIT","creditPrice":99,
        "previewMarkdown":"","fullMarkdown":"%s","requirements":"","deliverables":"",
        "version":"1.0","coverFileId":%s,"featured":true}
        """
                .formatted(markdown, cover == null ? "null" : "\"" + cover + "\"");
    }
}
