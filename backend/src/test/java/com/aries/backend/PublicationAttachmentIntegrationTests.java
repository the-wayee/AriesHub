package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class PublicationAttachmentIntegrationTests extends IntegrationTestSupport {
    @Test
    void resourceImagesAreListedAndCannotBypassPaidAccessThroughPreview() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        MockMultipartFile image =
                new MockMultipartFile(
                        "file",
                        "reference.png",
                        "image/png",
                        new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0});
        String uploaded =
                mvc.perform(
                                multipart("/api/v1/admin/media")
                                        .file(image)
                                        .param("kind", "IMAGE")
                                        .cookie(admin))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String fileId = JsonPath.read(uploaded, "$.data.id");
        String body =
                """
                {"categoryId":1,"title":"图片资源","summary":"图片附件","publicationType":"ARTICLE",
                "accessType":"CREDIT","creditPrice":50,"fullMarkdown":"正文",
                "previewMarkdown":"![图片](media:%s)","requirements":"","deliverables":"","attachmentIds":["%s"]}
                """
                        .formatted(fileId, fileId);
        String created =
                mvc.perform(
                                post("/api/v1/admin/publications")
                                        .cookie(admin)
                                        .contentType("application/json")
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String id = JsonPath.read(created, "$.data.id");
        mvc.perform(post("/api/v1/admin/publications/" + id + "/publish").cookie(admin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/publications/" + id + "/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].filename").value("reference.png"))
                .andExpect(jsonPath("$.data[0].locked").value(true));
        mvc.perform(get("/api/v1/publications/" + id + "/media/" + fileId + "/url"))
                .andExpect(status().isForbidden());
    }

    /** 即使附件链接放入试读，付费文章也禁止签名；移除绑定后旧文章下载入口失效。 */
    @Test
    void resourcesFollowArticleAccessAndRemoval() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        when(objects.downloadUrl(anyString(), anyString(), any()))
                .thenReturn("https://files.example/download?signature=test");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "prompts.txt",
                        "text/plain",
                        "AI video prompts".getBytes(StandardCharsets.UTF_8));
        String uploaded =
                mvc.perform(
                                multipart("/api/v1/admin/media")
                                        .file(file)
                                        .param("kind", "ATTACHMENT")
                                        .cookie(admin))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String fileId = JsonPath.read(uploaded, "$.data.id");
        String body =
                """
                {"categoryId":1,"title":"提示词附件","summary":"带附件的文章","publicationType":"ARTICLE",
                "accessType":"CREDIT","creditPrice":50,"fullMarkdown":"正文",
                "previewMarkdown":"[附件](media:%s)","requirements":"","deliverables":"","attachmentIds":["%s"]}
                """
                        .formatted(fileId, fileId);
        String created =
                mvc.perform(
                                post("/api/v1/admin/publications")
                                        .cookie(admin)
                                        .contentType("application/json")
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String id = JsonPath.read(created, "$.data.id");
        mvc.perform(get("/api/v1/publications/" + id + "/attachments"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/publications/" + id + "/publish").cookie(admin))
                .andExpect(status().isOk());
        String metadata =
                mvc.perform(get("/api/v1/publications/" + id + "/attachments"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].filename").value("prompts.txt"))
                        .andExpect(jsonPath("$.data[0].locked").value(true))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(metadata).doesNotContain("signature", "objectKey");
        mvc.perform(get("/api/v1/publications/" + id + "/media/" + fileId + "/url"))
                .andExpect(status().isForbidden());
        String freeBody =
                body.replace("CREDIT", "FREE").replace("\"creditPrice\":50", "\"creditPrice\":0");
        mvc.perform(
                        put("/api/v1/admin/publications/" + id)
                                .cookie(admin)
                                .contentType("application/json")
                                .content(freeBody))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/publications/" + id + "/media/" + fileId + "/url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").exists());
        mvc.perform(get("/api/v1/publications/" + id + "/attachments"))
                .andExpect(jsonPath("$.data[0].locked").value(false));
        String removed =
                freeBody.replace("[\"" + fileId + "\"]", "[]")
                        .replace("[附件](media:" + fileId + ")", "");
        mvc.perform(
                        put("/api/v1/admin/publications/" + id)
                                .cookie(admin)
                                .contentType("application/json")
                                .content(removed))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/publications/" + id + "/attachments"))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/v1/publications/" + id + "/media/" + fileId + "/url"))
                .andExpect(status().isNotFound());
    }
}
