package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aries.backend.catalog.application.port.PublicationAssetRepository;
import com.aries.backend.catalog.application.port.PublicationMediaPort.Asset;
import com.aries.backend.storage.application.service.FileStorageService;
import com.aries.backend.storage.domain.model.StoredFile;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.util.List;

/** 真实数据库与 HTTP 校验单篇授权、不重复计数、付费隔离和下架失效。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class PublicationShareIntegrationTests extends IntegrationTestSupport {
    @Autowired FileStorageService files;
    @Autowired PublicationAssetRepository assets;

    @Test
    void sharedMediaRemainsBoundToItsArticleAndPaidPreviewDoesNotUnlockAttachments()
            throws Exception {
        Cookie owner = register("share-media@example.com", "reader1234", "林舟");
        long user = database.userIdByEmail("share-media@example.com");
        StoredFile file =
                files.upload(
                        user,
                        StoredFile.Purpose.ATTACHMENT,
                        "prompt.txt",
                        MediaType.TEXT_PLAIN_VALUE,
                        4,
                        new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));
        String id = file.id().toString();
        assets.save(new Asset(id, user, "ATTACHMENT", "prompt.txt", MediaType.TEXT_PLAIN_VALUE, 4));
        assets.replaceBindings(11, List.of(id), List.of(), List.of(id));
        when(objects.downloadUrl(anyString(), anyString(), any()))
                .thenReturn("https://oss.example.com/signed");
        String free = token(11, owner);
        String paid = token(12, owner);
        mvc.perform(get("/api/v1/shares/" + free + "/media/" + id + "/url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://oss.example.com/signed"));
        mvc.perform(get("/api/v1/shares/" + free + "/attachments"))
                .andExpect(jsonPath("$.data[0].locked").value(false));
        mvc.perform(get("/api/v1/publications/11/media/" + id + "/url"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/shares/" + paid + "/media/" + id + "/url"))
                .andExpect(status().isNotFound());
        assets.replaceBindings(12, List.of(id), List.of(id), List.of(id));
        mvc.perform(get("/api/v1/shares/" + paid + "/media/" + id + "/url"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/shares/" + paid + "/media/" + id + "/url").cookie(owner))
                .andExpect(status().isForbidden());
        database.unlock(user, 12);
        mvc.perform(get("/api/v1/shares/" + paid + "/media/" + id + "/url").cookie(owner))
                .andExpect(status().isOk());
    }

    private String token(long id, Cookie cookie) throws Exception {
        return JsonPath.read(
                mvc.perform(post("/api/v1/publications/" + id + "/share-link").cookie(cookie))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.token");
    }

    @Test
    void freeGrantDoesNotAuthorizeOtherArticlesAndRepeatedCopiesCountOnce() throws Exception {
        Cookie owner = register("share-owner@example.com", "reader1234", "林舟");
        Cookie other = register("share-other@example.com", "reader1234", "程雨");
        long userId = database.userIdByEmail("share-owner@example.com");
        StoredFile avatar =
                files.upload(
                        userId,
                        StoredFile.Purpose.AVATAR,
                        "avatar.png",
                        MediaType.IMAGE_PNG_VALUE,
                        4,
                        new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));
        when(objects.imageUrl(anyString(), any())).thenReturn("https://oss.example.com/avatar");
        mvc.perform(
                        put("/api/v1/users/me/profile")
                                .cookie(owner)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nickname\":\"林舟\",\"bio\":\"\",\"avatarFileId\":\""
                                                + avatar.id()
                                                + "\"}"))
                .andExpect(status().isOk());
        String grant = token(11, owner);
        assertThat(token(11, owner)).isEqualTo(grant);
        mvc.perform(get("/api/v1/shares/" + grant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sharedBy").value("林舟"))
                .andExpect(
                        jsonPath("$.data.sharedAvatarUrl").value("https://oss.example.com/avatar"))
                .andExpect(jsonPath("$.data.canRead").value(true));
        mvc.perform(get("/api/v1/shares/" + grant + "/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markdown").value("FREE_BODY"));
        mvc.perform(get("/api/v1/publications/11/content")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/publications/12/content").param("shareToken", grant))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/shares/" + "x".repeat(43) + "/content"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/publications/11/share-link"))
                .andExpect(status().isUnauthorized());
        for (int i = 0; i < 8; i++)
            mvc.perform(
                            post("/api/v1/publications/11/share")
                                    .cookie(owner)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"token\":\"" + grant + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.shareCount").value(1));
        mvc.perform(
                        post("/api/v1/publications/11/share")
                                .cookie(other)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"" + grant + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/home/activity").cookie(owner))
                .andExpect(jsonPath("$.data.length()").value(1));
        database.updatePublicationStatus(11, "ARCHIVED");
        mvc.perform(get("/api/v1/shares/" + grant + "/content")).andExpect(status().isNotFound());
    }

    @Test
    void paidGrantRequiresLoginAndAnExistingUnlockForTheCurrentUser() throws Exception {
        Cookie owner = register("paid-owner@example.com", "reader1234", "林舟");
        Cookie other = register("paid-other@example.com", "reader1234", "程雨");
        String grant = token(12, owner);
        mvc.perform(get("/api/v1/shares/" + grant))
                .andExpect(jsonPath("$.data.canRead").value(false));
        mvc.perform(get("/api/v1/shares/" + grant + "/content"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/shares/" + grant + "/content").cookie(owner))
                .andExpect(status().isForbidden());
        String profile =
                mvc.perform(get("/api/v1/users/me").cookie(owner))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String userId = JsonPath.read(profile, "$.data.id");
        database.unlock(Long.parseLong(userId), 12);
        mvc.perform(get("/api/v1/shares/" + grant).cookie(owner))
                .andExpect(jsonPath("$.data.canRead").value(true));
        mvc.perform(get("/api/v1/shares/" + grant + "/content").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markdown").value("CREDIT_SECRET_SENTINEL"));
        mvc.perform(get("/api/v1/shares/" + grant + "/content").cookie(other))
                .andExpect(status().isForbidden());
    }
}
