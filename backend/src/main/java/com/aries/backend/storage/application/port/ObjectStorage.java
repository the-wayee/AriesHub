package com.aries.backend.storage.application.port;

import java.io.InputStream;
import java.time.Duration;

/** 对象存储边界，SDK 和供应商只存在于基础设施层。 */
public interface ObjectStorage {
    void put(String key, InputStream content, long size, String contentType);
    void delete(String key);
    String downloadUrl(String key, String filename, Duration ttl);
}
