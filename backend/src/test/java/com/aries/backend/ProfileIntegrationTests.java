package com.aries.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class ProfileIntegrationTests extends IntegrationTestSupport {
    @org.springframework.beans.factory.annotation.Autowired com.aries.backend.storage.application.service.FileStorageService fileStorage;
    @Test void profileRequiresLoginAndPersistsWithoutChangingIdentity() throws Exception {
        mvc.perform(put("/api/v1/users/me/profile").contentType("application/json")
                .content("{\"nickname\":\"新名字\",\"bio\":\"AI 实践者\"}"))
                .andExpect(status().isUnauthorized());
        Cookie owner = register("profile@example.com", "profile123", "原昵称");
        mvc.perform(put("/api/v1/users/me/profile").cookie(owner).contentType("application/json")
                .content("{\"nickname\":\" 新昵称 \",\"bio\":\" 探索 AI 与设计 \",\"avatarFileId\":null,\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nickname").value("新昵称"))
                .andExpect(jsonPath("$.bio").value("探索 AI 与设计"))
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/users/me").cookie(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bio").value("探索 AI 与设计"));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"profile@example.com\",\"password\":\"profile123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nickname").value("新昵称"));
    }

    @Test void invalidProfilesDoNotChangeSavedData() throws Exception {
        Cookie owner = register("validation@example.com", "profile123", "原昵称");
        for (String body : new String[]{
                "{\"nickname\":\" x \",\"bio\":\"签名\"}",
                "{\"nickname\":\"合法昵称\",\"bio\":\"" + "字".repeat(161) + "\"}",
                "{\"nickname\":\"合法昵称\",\"bio\":null}"}) {
            mvc.perform(put("/api/v1/users/me/profile").cookie(owner).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/users/me").cookie(owner))
                .andExpect(jsonPath("$.nickname").value("原昵称"));
    }

    @Test void avatarBindingChecksOwnerPurposeAndSupportsRemoval() throws Exception {
        Cookie owner = register("avatar@example.com", "profile123", "头像成员");
        var uploaded = mvc.perform(multipart("/api/v1/users/me/avatar").cookie(owner)
                .file(new MockMultipartFile("file", "avatar.png", "image/png",
                        new byte[]{(byte)137,80,78,71,13,10,26,10,0,0,0,0})))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = uploaded.split("\"id\":\"")[1].split("\"")[0];
        String body = "{\"nickname\":\"头像成员\",\"bio\":\"\",\"avatarFileId\":\"" + id + "\"}";
        mvc.perform(put("/api/v1/users/me/profile").cookie(owner).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatarFileId").value(id));
        when(objects.imageUrl(anyString(), any())).thenReturn("https://files.example.com/avatar.png");
        mvc.perform(get("/api/v1/users/me/avatar-url").cookie(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("https://files.example.com/avatar.png"))
                .andExpect(jsonPath("$.expiresAt").exists()).andExpect(header().string("Cache-Control", "no-store"));
        Cookie stranger = register("stranger-profile@example.com", "profile123", "其他成员");
        mvc.perform(put("/api/v1/users/me/profile").cookie(stranger).contentType("application/json").content(body))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/me/avatar-url").cookie(stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/me/avatar-url")).andExpect(status().isUnauthorized());
        String attachmentId = fileStorage.upload(database.userIdByEmail("avatar@example.com"),
                com.aries.backend.storage.domain.model.StoredFile.Purpose.ATTACHMENT, "notes.pdf", "application/pdf", 8,
                new java.io.ByteArrayInputStream("%PDF-1.7".getBytes())).id().toString();
        for (String rejectedId : new String[]{attachmentId, UUID.randomUUID().toString()}) {
            mvc.perform(put("/api/v1/users/me/profile").cookie(owner).contentType("application/json")
                    .content(body.replace(id, rejectedId))).andExpect(status().isNotFound());
        }
        mvc.perform(put("/api/v1/users/me/profile").cookie(owner).contentType("application/json")
                .content("{\"nickname\":\"头像成员\",\"bio\":\"\",\"avatarFileId\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatarFileId").doesNotExist());
        mvc.perform(get("/api/v1/users/me/avatar-url").cookie(owner)).andExpect(status().isNotFound());
        verify(objects, times(1)).imageUrl(anyString(), any());
    }
}
