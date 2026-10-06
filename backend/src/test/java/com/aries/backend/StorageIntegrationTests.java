package com.aries.backend;

import com.aries.backend.storage.domain.repository.StoredFileRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class StorageIntegrationTests extends IntegrationTestSupport {
    @Autowired StoredFileRepository files;
    @Autowired com.aries.backend.storage.application.service.FileStorageService fileStorage;
    private MockMultipartFile png() {
        return new MockMultipartFile("file", "avatar.png", "image/png",
                new byte[]{(byte)137,80,78,71,13,10,26,10,0,0,0,0});
    }
    @Test void uploadRequiresLogin() throws Exception {
        mvc.perform(multipart("/api/v1/users/me/avatar").file(png()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(objects);
    }
    @Test void uploadsPersistMetadataAndDownloadsAreOwnerOnly() throws Exception {
        Cookie owner = register("owner@example.com", "storage123", "上传者");
        var response = mvc.perform(multipart("/api/v1/users/me/avatar").file(png())
                        .cookie(owner))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purpose").value("AVATAR"))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        String id = response.split("\"id\":\"")[1].split("\"")[0];
        var stored = files.findById(UUID.fromString(id)).orElseThrow();
        assertThat(stored.ownerId()).isEqualTo(database.userIdByEmail("owner@example.com"));
        assertThat(stored.objectKey()).startsWith("uploads/" + stored.ownerId() + "/avatar/");
        verify(objects).put(eq(stored.objectKey()), any(InputStream.class), eq(12L), eq("image/png"));
        when(objects.downloadUrl(eq(stored.objectKey()), eq("avatar.png"), eq(Duration.ofMinutes(5))))
                .thenReturn("https://files.example.com/temporary-link");
        mvc.perform(get("/api/v1/storage/files/" + id + "/download-url").cookie(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("https://files.example.com/temporary-link"))
                .andExpect(jsonPath("$.expiresAt").exists());
        Cookie stranger = register("stranger@example.com", "storage123", "其他用户");
        mvc.perform(get("/api/v1/storage/files/" + id + "/download-url").cookie(stranger))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
        mvc.perform(get("/api/v1/storage/files/" + id + "/download-url"))
                .andExpect(status().isUnauthorized());
        verify(objects, times(1)).downloadUrl(anyString(), anyString(), any());
    }
    @Test void forgedImagesAndNonImageAvatarsAreRejected() throws Exception {
        Cookie owner = register("invalid@example.com", "storage123", "上传者");
        for (var file : new MockMultipartFile[]{
                new MockMultipartFile("file", "fake.png", "image/png", "not a png".getBytes()),
                new MockMultipartFile("file", "document.pdf", "application/pdf", "%PDF-test".getBytes())}) {
            mvc.perform(multipart("/api/v1/users/me/avatar").file(file).cookie(owner))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(objects);
    }
    @Test void genericStorageSupportsAttachmentsAndAvatarUploadsAreRateLimited() throws Exception {
        Cookie owner = register("limited@example.com", "storage123", "上传者");
        var attachment = fileStorage.upload(database.userIdByEmail("limited@example.com"),
                com.aries.backend.storage.domain.model.StoredFile.Purpose.ATTACHMENT,
                "notes.pdf", "application/pdf", 13, new java.io.ByteArrayInputStream("%PDF-1.7 test".getBytes()));
        assertThat(attachment.purpose()).isEqualTo(com.aries.backend.storage.domain.model.StoredFile.Purpose.ATTACHMENT);
        for (int i = 0; i < 20; i++) {
            mvc.perform(multipart("/api/v1/users/me/avatar").file(png()).cookie(owner)).andExpect(status().isCreated());
        }
        mvc.perform(multipart("/api/v1/users/me/avatar").file(png()).cookie(owner))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
        verify(objects, times(21)).put(anyString(), any(), anyLong(), anyString());
    }
    @Test void providerFailureDoesNotCreateFileMetadata() throws Exception {
        Cookie owner = register("failure@example.com", "storage123", "上传者");
        doThrow(new com.aries.backend.shared.application.exception.BusinessException(
                com.aries.backend.storage.application.exception.StorageErrorCode.STORAGE_UNAVAILABLE))
                .when(objects).put(anyString(), any(), anyLong(), anyString());
        mvc.perform(multipart("/api/v1/users/me/avatar").file(png()).cookie(owner))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
        assertThat(database.storedFileCount()).isZero();
    }
}
