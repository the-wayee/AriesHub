package com.aries.backend.catalog.application.port;

import java.io.InputStream;
import java.time.Instant;

public interface PublicationMediaPort {
    record Asset(
            String id, long ownerId, String kind, String filename, String contentType, long size) {}

    record SignedUrl(String url, Instant expiresAt) {}

    long currentUserId();

    void checkRate(long userId);

    Asset upload(
            long userId, String kind, String filename, String type, long size, InputStream content);

    SignedUrl url(Asset asset);
}
