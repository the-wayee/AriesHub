package com.aries.backend.composition;

import com.aries.backend.identity.application.port.UserAvatarStorage;
import com.aries.backend.storage.application.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.io.InputStream;
import com.aries.backend.storage.application.port.UploadRateLimiter;
import com.aries.backend.storage.domain.model.StoredFile;

/** 仅桥接用户头像操作，不扩大附件下载权限。 */
@Component
@RequiredArgsConstructor
public class UserAvatarStorageAdapter implements UserAvatarStorage {
    private final FileStorageService files;
    private final UploadRateLimiter rateLimiter;
    public File metadata(UUID fileId) { return view(files.metadata(fileId)); }
    public File upload(long userId, String filename, String contentType, long size, InputStream content) {
        return view(files.upload(userId, StoredFile.Purpose.AVATAR, filename, contentType, size, content));
    }
    public void checkUploadRate(long userId) { rateLimiter.check(userId); }
    public Image inlineUrl(UUID fileId) {
        var image = files.inlineUrl(files.metadata(fileId));
        return new Image(image.url(), image.expiresAt());
    }
    private File view(StoredFile file) {
        return new File(file.id(), file.ownerId(), file.purpose().name(), file.filename(), file.contentType(), file.size());
    }
}
