package com.aries.backend.identity.application.service;

import com.aries.backend.identity.application.port.UserAvatarStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.aries.backend.identity.application.port.SessionManager;
import com.aries.backend.identity.domain.repository.UserRepository;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.model.Email;
import java.util.Optional;
import java.io.ByteArrayInputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserApplicationServiceTest {
    private final UserAvatarStorage storage = mock(UserAvatarStorage.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SessionManager sessions = mock(SessionManager.class);
    private final UserApplicationService service = new UserApplicationService(users, sessions, storage);

    @BeforeEach void signedInUser() {
        when(sessions.currentUserId()).thenReturn(42L);
        when(users.findById(42L)).thenReturn(Optional.of(
                UserAccount.builder().id(42L)
                    .email(new Email("member@example.com"))
                    .status(UserAccount.Status.ACTIVE).build()));
    }

    @Test void businessLimitsRejectInvalidAvatarsBeforeStorage() {
        for (long size : new long[]{0, 5L * 1024 * 1024 + 1}) {
            assertThatThrownBy(() -> service.uploadAvatar("a.png", "image/png", size,
                    new ByteArrayInputStream(new byte[0]))).hasMessageContaining("最大 5 MiB");
        }
        assertThatThrownBy(() -> service.uploadAvatar("a.pdf", "application/pdf", 12,
                new ByteArrayInputStream("%PDF-test".getBytes()))).hasMessageContaining("最大 5 MiB");
        verifyNoInteractions(storage);
    }

    @Test void formatValidationDoesNotConsumeBytesBeforeStorage() {
        byte[] png = new byte[]{(byte)137,80,78,71,13,10,26,10,1,2,3,4,5};
        when(storage.upload(eq(42L), eq("a.png"), eq("image/png"), eq((long)png.length), any()))
                .thenAnswer(call -> {
                    assertThat(((java.io.InputStream)call.getArgument(4)).readAllBytes()).isEqualTo(png);
                    return null;
                });
        service.uploadAvatar("a.png", "image/png", png.length, new ByteArrayInputStream(png));
        verify(storage).checkUploadRate(42);
        verify(storage).upload(eq(42L), any(), any(), anyLong(), any());
    }
}
