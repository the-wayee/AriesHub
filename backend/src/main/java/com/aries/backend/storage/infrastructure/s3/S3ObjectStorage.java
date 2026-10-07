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
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class S3ObjectStorage implements ObjectStorage {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    @Override
    public void put(String key, InputStream content, long size, String type) {
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(type)
                            .contentLength(size)
                            .build(),
                    RequestBody.fromInputStream(content, size));
        } catch (SdkException error) {
            throw unavailable("put", key, error);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException error) {
            throw unavailable("delete", key, error);
        }
    }

    @Override
    public String downloadUrl(String key, String filename, Duration ttl) {
        try {
            // attachment 避免不可信附件被浏览器当作同源页面执行。
            String disposition =
                    ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8)
                            .build()
                            .toString();
            return presigner
                    .presignGetObject(
                            GetObjectPresignRequest.builder()
                                    .signatureDuration(ttl)
                                    .getObjectRequest(
                                            GetObjectRequest.builder()
                                                    .bucket(bucket)
                                                    .key(key)
                                                    .responseContentDisposition(disposition)
                                                    .build())
                                    .build())
                    .url()
                    .toString();
        } catch (SdkException error) {
            throw unavailable("presign", key, error);
        }
    }

    private BusinessException unavailable(String operation, String key, SdkException error) {
        // 不输出 SDK 的完整请求，避免凭据或签名 URL 进入日志。
        log.error(
                "对象存储操作失败: operation={}, key={}, exception={}",
                operation,
                key,
                error.getClass().getSimpleName());
        return new BusinessException(StorageErrorCode.STORAGE_UNAVAILABLE);
    }

    private static final int PART_BYTES =
            (int) org.springframework.util.unit.DataSize.ofMegabytes(5).toBytes();

    /** 分片成功响应后才累计进度；重试、读取本地流和合并中的字节均不提前算成功。 */
    @Override
    public void put(
            String key,
            InputStream content,
            long size,
            String type,
            java.util.function.LongConsumer confirmedBytes) {
        if (size <= PART_BYTES) {
            put(key, content, size, type);
            try {
                confirmedBytes.accept(size);
            } catch (RuntimeException error) {
                try {
                    delete(key);
                } catch (RuntimeException cleanup) {
                    error.addSuppressed(cleanup);
                }
                throw error;
            }
            return;
        }
        String uploadId = null;
        try {
            uploadId =
                    client.createMultipartUpload(
                                    CreateMultipartUploadRequest.builder()
                                            .bucket(bucket)
                                            .key(key)
                                            .contentType(type)
                                            .build())
                            .uploadId();
            List<CompletedPart> parts = new ArrayList<>();
            long confirmed = 0;
            while (confirmed < size) {
                // 最多缓存一个 5 MiB 分片，失败时 SDK 可安全重试同一份字节。
                confirmedBytes.accept(confirmed);
                byte[] bytes = content.readNBytes((int) Math.min(PART_BYTES, size - confirmed));
                if (bytes.length != Math.min(PART_BYTES, size - confirmed))
                    throw new java.io.IOException("Upload stream ended early");
                int number = parts.size() + 1;
                UploadPartResponse response =
                        client.uploadPart(
                                UploadPartRequest.builder()
                                        .bucket(bucket)
                                        .key(key)
                                        .uploadId(uploadId)
                                        .partNumber(number)
                                        .contentLength((long) bytes.length)
                                        .build(),
                                RequestBody.fromBytes(bytes));
                parts.add(CompletedPart.builder().partNumber(number).eTag(response.eTag()).build());
                confirmed += bytes.length;
                confirmedBytes.accept(confirmed);
            }
            client.completeMultipartUpload(
                    CompleteMultipartUploadRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .uploadId(uploadId)
                            .multipartUpload(
                                    CompletedMultipartUpload.builder().parts(parts).build())
                            .build());
        } catch (java.io.IOException | RuntimeException error) {
            if (uploadId != null) {
                try {
                    client.abortMultipartUpload(
                            AbortMultipartUploadRequest.builder()
                                    .bucket(bucket)
                                    .key(key)
                                    .uploadId(uploadId)
                                    .build());
                } catch (RuntimeException cleanup) {
                    error.addSuppressed(cleanup);
                }
            }
            if (error instanceof BusinessException business) throw business;
            if (error instanceof SdkException sdk) throw unavailable("multipart-put", key, sdk);
            throw new BusinessException(StorageErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    @Override
    public String imageUrl(String key, Duration ttl) {
        try {
            return presigner
                    .presignGetObject(
                            GetObjectPresignRequest.builder()
                                    .signatureDuration(ttl)
                                    .getObjectRequest(
                                            GetObjectRequest.builder()
                                                    .bucket(bucket)
                                                    .key(key)
                                                    .responseContentDisposition("inline")
                                                    .build())
                                    .build())
                    .url()
                    .toString();
        } catch (SdkException error) {
            throw unavailable("presign-image", key, error);
        }
    }
}
