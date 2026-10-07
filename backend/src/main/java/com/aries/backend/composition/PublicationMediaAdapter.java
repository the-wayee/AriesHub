package com.aries.backend.composition;

import com.aries.backend.catalog.application.port.PublicationMediaPort;
import com.aries.backend.catalog.domain.model.PublicationMediaKind;
import com.aries.backend.identity.application.service.UserApplicationService;
import com.aries.backend.storage.application.port.UploadRateLimiter;
import com.aries.backend.storage.application.service.FileStorageService;
import com.aries.backend.storage.domain.model.StoredFile;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;

/** 桥接内容素材端口与身份、通用文件工具；不在存储模块增加内容业务。 */
@Component
@RequiredArgsConstructor
public class PublicationMediaAdapter implements PublicationMediaPort {
    private final UserApplicationService users;
    private final FileStorageService files;
    private final UploadRateLimiter limiter;

    public long currentUserId() {
        return Long.parseLong(users.currentUser().id());
    }

    public void checkRate(long id) {
        limiter.check(id);
    }

    public Asset upload(
            long userId,
            String kind,
            String filename,
            String type,
            long size,
            InputStream content) {
        return upload(userId, kind, filename, type, size, content, ignored -> {});
    }

    public Asset upload(
            long userId,
            String kind,
            String filename,
            String type,
            long size,
            InputStream content,
            java.util.function.LongConsumer confirmedBytes) {
        StoredFile file =
                files.upload(
                        userId,
                        StoredFile.Purpose.ATTACHMENT,
                        filename,
                        type,
                        size,
                        content,
                        confirmedBytes);
        return new Asset(file.id().toString(), userId, kind, file.filename(), type, size);
    }

    public SignedUrl url(Asset asset) {
        StoredFile file = files.metadata(UUID.fromString(asset.id()));
        FileStorageService.Download signed =
                PublicationMediaKind.ATTACHMENT.name().equals(asset.kind())
                        ? files.downloadUrl(file)
                        : files.inlineUrl(file);
        return new SignedUrl(signed.url(), signed.expiresAt());
    }
}
