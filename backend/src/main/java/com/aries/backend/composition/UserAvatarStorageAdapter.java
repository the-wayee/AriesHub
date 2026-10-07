package com.aries.backend.composition;

import com.aries.backend.identity.application.port.UserAvatarStorage;
import com.aries.backend.storage.application.port.UploadRateLimiter;
import com.aries.backend.storage.application.service.FileStorageService;
import com.aries.backend.storage.domain.model.StoredFile;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** 仅桥接用户头像操作，不扩大附件下载权限。 */
@Component
@RequiredArgsConstructor
public class UserAvatarStorageAdapter implements UserAvatarStorage {
    private final FileStorageService files;
    private final UploadRateLimiter rateLimiter;

    public File metadata(UUID fileId) {
        return view(files.metadata(fileId));
    }

    public File upload(
            long userId, String filename, String contentType, long size, InputStream content) {
        return view(
                files.upload(
                        userId, StoredFile.Purpose.AVATAR, filename, contentType, size, content));
    }

    public void checkUploadRate(long userId) {
        rateLimiter.check(userId);
    }

    public Image inlineUrl(UUID fileId) {
        FileStorageService.Download image = files.inlineUrl(files.metadata(fileId));
        return new Image(image.url(), image.expiresAt());
    }

    public Map<Long, Image> publicAvatars(Map<Long, UUID> avatarIds) {
        if (avatarIds.isEmpty()) return Map.of();
        Map<UUID, StoredFile> stored =
                files.metadata(new ArrayList<>(avatarIds.values())).stream()
                        .collect(Collectors.toMap(StoredFile::id, file -> file));
        Map<Long, Image> result = new HashMap<>();
        avatarIds.forEach(
                (user, id) -> {
                    StoredFile file = stored.get(id);
                    // 公开身份仅暴露本人头像，不能拿头像引用签发别人的附件地址。
                    if (file != null
                            && file.ownerId() == user
                            && file.purpose() == StoredFile.Purpose.AVATAR) {
                        FileStorageService.Download signed = files.inlineUrl(file);
                        result.put(user, new Image(signed.url(), signed.expiresAt()));
                    }
                });
        return result;
    }

    private File view(StoredFile file) {
        return new File(
                file.id(),
                file.ownerId(),
                file.purpose().name(),
                file.filename(),
                file.contentType(),
                file.size());
    }
}
