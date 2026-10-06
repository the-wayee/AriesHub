package com.aries.backend.storage.infrastructure.s3;

import com.aries.backend.shared.application.exception.BusinessException;
import com.aries.backend.storage.application.exception.StorageErrorCode;
import com.aries.backend.storage.application.port.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class S3ObjectStorage implements ObjectStorage {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;
    @Override public void put(String key, InputStream content, long size, String type) {
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(type)
                    .contentLength(size).build(), RequestBody.fromInputStream(content, size));
        } catch (SdkException error) { throw unavailable("put", key, error); }
    }
    @Override public void delete(String key) {
        try { client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build()); }
        catch (SdkException error) { throw unavailable("delete", key, error); }
    }
    @Override public String downloadUrl(String key, String filename, Duration ttl) {
        try {
            // attachment 避免不可信附件被浏览器当作同源页面执行。
            String disposition = ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString();
            return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(ttl)
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key)
                            .responseContentDisposition(disposition).build()).build()).url().toString();
        } catch (SdkException error) { throw unavailable("presign", key, error); }
    }
    private BusinessException unavailable(String operation, String key, SdkException error) {
        // 不输出 SDK 的完整请求，避免凭据或签名 URL 进入日志。
        log.error("对象存储操作失败: operation={}, key={}, exception={}", operation, key, error.getClass().getSimpleName());
        return new BusinessException(StorageErrorCode.STORAGE_UNAVAILABLE);
    }

    @Override public String imageUrl(String key, Duration ttl) {
        try {
            return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(ttl)
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key)
                            .responseContentDisposition("inline").build()).build()).url().toString();
        } catch (SdkException error) { throw unavailable("presign-image", key, error); }
    }
}
