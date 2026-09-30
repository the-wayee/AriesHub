package com.aries.backend.storage.application.service;

import com.aries.backend.storage.application.port.*;
import com.aries.backend.storage.domain.model.StoredFile;
import com.aries.backend.storage.domain.repository.StoredFileRepository;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FileStorageServiceTest {
    private final ObjectStorage objects = mock(ObjectStorage.class);
    private final StoredFileRepository files = mock(StoredFileRepository.class);
    private final FileStorageService service = new FileStorageService(() -> 42L, user -> {}, objects, files);
    @Test void failedMetadataSaveRemovesUploadedObject() {
        var failure = new IllegalStateException("metadata write failed");
        doThrow(failure).when(files).save(any());
        assertThatThrownBy(() -> service.upload(StoredFile.Purpose.ATTACHMENT, "../说明.pdf", "application/pdf", 8,
                new ByteArrayInputStream("%PDF-1.7".getBytes()))).isSameAs(failure);
        var order = inOrder(objects, files);
        order.verify(objects).put(startsWith("uploads/42/attachment/"), any(), eq(8L), eq("application/pdf"));
        order.verify(files).save(argThat(file -> file.filename().equals("说明.pdf")));
        order.verify(objects).delete(startsWith("uploads/42/attachment/"));
    }
    @Test void fileLimitsApplyBeforeCallingProvider() {
        for (long size : new long[]{0, FileStorageService.AVATAR_MAX_BYTES + 1}) {
            assertThatThrownBy(() -> service.upload(StoredFile.Purpose.AVATAR, "avatar.png", "image/png", size,
                    new ByteArrayInputStream(new byte[0]))).hasMessage("文件类型、内容或大小不符合要求");
        }
        assertThatThrownBy(() -> service.upload(StoredFile.Purpose.ATTACHMENT, "a.zip", "application/zip",
                FileStorageService.ATTACHMENT_MAX_BYTES + 1, new ByteArrayInputStream(new byte[0])))
                .hasMessage("文件类型、内容或大小不符合要求");
        verifyNoInteractions(objects, files);
    }
    @Test void headerValidationPreservesUploadedBytes() throws Exception {
        byte[] content = "%PDF-1.7 contents beyond the prefix".getBytes();
        doAnswer(call -> {
            assertThat(((java.io.InputStream) call.getArgument(1)).readAllBytes()).isEqualTo(content);
            return null;
        }).when(objects).put(anyString(), any(), anyLong(), anyString());
        service.upload(StoredFile.Purpose.ATTACHMENT, "notes.pdf", "application/pdf", content.length,
                new ByteArrayInputStream(content));
        verify(files).save(any());
    }
}
