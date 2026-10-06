package com.aries.backend.storage.application.port;

import java.io.InputStream;
import java.time.Duration;

/** 对象存储边界，SDK 和供应商只存在于基础设施层。 */
public interface ObjectStorage {
    void put(String key, InputStream content, long size, String contentType);

    /** 回调为存储服务已确认的字节数，不能把读取本地流的字节当作上传成功。 */
    default void put(
            String key,
            InputStream content,
            long size,
            String contentType,
            java.util.function.LongConsumer confirmedBytes) {
        put(key, content, size, contentType);
        confirmedBytes.accept(size);
    }

    void delete(String key);

    String downloadUrl(String key, String filename, Duration ttl);

    String imageUrl(String key, Duration ttl);
}
