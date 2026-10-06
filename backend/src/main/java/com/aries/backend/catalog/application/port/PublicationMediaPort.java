package com.aries.backend.catalog.application.port;

import java.io.InputStream;
import java.time.Instant;

/** 文章素材存取端口；由 composition 桥接通用存储，签名授权由内容用例先完成。 */
public interface PublicationMediaPort {
    record Asset(
            String id, long ownerId, String kind, String filename, String contentType, long size) {}

    record SignedUrl(String url, Instant expiresAt) {}

    long currentUserId();

    void checkRate(long userId);

    Asset upload(
            long userId, String kind, String filename, String type, long size, InputStream content);

    default Asset upload(
            long userId,
            String kind,
            String filename,
            String type,
            long size,
            InputStream content,
            java.util.function.LongConsumer confirmedBytes) {
        Asset asset = upload(userId, kind, filename, type, size, content);
        confirmedBytes.accept(size);
        return asset;
    }

    SignedUrl url(Asset asset);
}
