package com.aries.backend.identity.application.service;

import com.aries.backend.identity.application.port.UserAvatarStorage;
import com.aries.backend.identity.application.port.SessionManager;
import com.aries.backend.identity.application.view.IdentityViews.CurrentUser;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.repository.UserRepository;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import static com.aries.backend.identity.application.exception.IdentityErrorCode.*;

/** 用户查询、资料修改及头像用例；头像是用户资料的一部分。 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private final UserRepository users;
    private final SessionManager sessions;
    private final UserAvatarStorage storage;

    public UserAvatarStorage.File uploadAvatar(String filename, String type, long size, InputStream content) {
        long userId = currentAccount().getId();
        if (type == null || !TYPES.contains(type) || size <= 0 || size > MAX_BYTES)
            throw new BusinessException(INVALID_AVATAR);
        storage.checkUploadRate(userId);
        try {
            var stream = new PushbackInputStream(content, 12);
            byte[] header = stream.readNBytes(12);
            if (!matches(type, header)) throw new BusinessException(INVALID_AVATAR);
            stream.unread(header);
            return storage.upload(userId, filename, type, size, stream);
        } catch (IOException error) {
            throw new BusinessException(INVALID_AVATAR);
        }
    }

    private void verifyOwnership(UUID fileId, long userId) {
        var file = storage.metadata(fileId);
        if (file.ownerId() != userId || !"AVATAR".equals(file.purpose()) || !TYPES.contains(file.contentType()))
            throw new BusinessException(AVATAR_FILE_NOT_FOUND);
    }

    private boolean matches(String type, byte[] h) {
        return switch (type) {
            case "image/png" -> h.length >= 8 && Arrays.equals(Arrays.copyOf(h, 8),
                    new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10});
            case "image/jpeg" -> h.length >= 3 && h[0] == (byte)255 && h[1] == (byte)216 && h[2] == (byte)255;
            case "image/webp" -> h.length >= 12 && ascii(h, 0, "RIFF") && ascii(h, 8, "WEBP");
            default -> false;
        };
    }
    private boolean ascii(byte[] header, int offset, String value) {
        for (int i = 0; i < value.length(); i++) if (header[offset + i] != value.charAt(i)) return false;
        return true;
    }

    @Transactional(readOnly = true)
    public CurrentUser currentUser() {
        return CurrentUser.from(currentAccount());
    }

    private UserAccount currentAccount() {
        long userId = sessions.currentUserId();
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        if (!user.canLogin()) {
            sessions.logout();
            throw new BusinessException(ACCOUNT_DISABLED);
        }
        return user;
    }

    @Transactional
    public CurrentUser updateProfile(String nickname, String bio, UUID avatarFileId) {
        UserAccount current = currentAccount();
        String name = nickname == null ? "" : nickname.trim();
        String signature = bio == null ? "" : bio.trim();
        if (name.length() < 2 || name.length() > 30 || signature.length() > 160) {
            throw new BusinessException(INVALID_PROFILE);
        }
        long userId = current.getId();
        if (avatarFileId != null) verifyOwnership(avatarFileId, userId);
        UserAccount updated = current.editProfile(name, signature, avatarFileId);
        users.updateProfile(updated);
        return CurrentUser.from(updated);
    }

    @Transactional(readOnly = true)
    public UserAvatarStorage.Image avatarImage() {
        UserAccount user = currentAccount();
        if (user.getAvatarFileId() == null) throw new BusinessException(AVATAR_NOT_SET);
        verifyOwnership(user.getAvatarFileId(), user.getId());
        return storage.inlineUrl(user.getAvatarFileId());
    }
}
