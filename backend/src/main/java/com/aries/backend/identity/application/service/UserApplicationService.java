package com.aries.backend.identity.application.service;

import static com.aries.backend.identity.application.exception.IdentityErrorCode.*;
import static com.aries.backend.shared.application.util.MediaTypes.IMAGE_WEBP_VALUE;

import static org.springframework.util.MimeTypeUtils.IMAGE_JPEG_VALUE;
import static org.springframework.util.MimeTypeUtils.IMAGE_PNG_VALUE;

import com.aries.backend.identity.application.port.SessionManager;
import com.aries.backend.identity.application.port.UserAvatarStorage;
import com.aries.backend.identity.application.view.IdentityViews.CurrentUser;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.repository.UserRepository;
import com.aries.backend.shared.application.exception.BusinessException;
import com.aries.backend.shared.application.util.FileSignatures;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Set;
import java.util.UUID;

/** 用户查询、资料修改及头像用例；头像是用户资料的一部分。 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {
    private static final long MAX_AVATAR_BYTES = DataSize.ofMegabytes(5).toBytes();
    private static final Set<String> TYPES =
            Set.of(IMAGE_PNG_VALUE, IMAGE_JPEG_VALUE, IMAGE_WEBP_VALUE);
    private final UserRepository users;
    private final SessionManager sessions;
    private final UserAvatarStorage storage;

    public UserAvatarStorage.File uploadAvatar(
            String filename, String type, long size, InputStream content) {
        long userId = currentAccount().getId();
        if (type == null || !TYPES.contains(type) || size <= 0 || size > MAX_AVATAR_BYTES)
            throw new BusinessException(INVALID_AVATAR);
        storage.checkUploadRate(userId);
        try {
            PushbackInputStream stream =
                    new PushbackInputStream(content, FileSignatures.HEADER_BYTES);
            byte[] header = stream.readNBytes(FileSignatures.HEADER_BYTES);
            if (!FileSignatures.matchesImage(type, header))
                throw new BusinessException(INVALID_AVATAR);
            stream.unread(header);
            return storage.upload(userId, filename, type, size, stream);
        } catch (IOException error) {
            throw new BusinessException(INVALID_AVATAR);
        }
    }

    private void verifyOwnership(UUID fileId, long userId) {
        UserAvatarStorage.File file = storage.metadata(fileId);
        if (file.ownerId() != userId
                || !UserAvatarStorage.AVATAR_PURPOSE.equals(file.purpose())
                || !TYPES.contains(file.contentType()))
            throw new BusinessException(AVATAR_FILE_NOT_FOUND);
    }

    @Transactional(readOnly = true)
    public CurrentUser currentUser() {
        return CurrentUser.from(currentAccount());
    }

    private UserAccount currentAccount() {
        long userId = sessions.currentUserId();
        UserAccount user =
                users.findById(userId).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
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
