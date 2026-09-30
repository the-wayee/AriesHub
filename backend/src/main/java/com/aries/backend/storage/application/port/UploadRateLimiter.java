package com.aries.backend.storage.application.port;

public interface UploadRateLimiter {
    void check(long userId);
}
