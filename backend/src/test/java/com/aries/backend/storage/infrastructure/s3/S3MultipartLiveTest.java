package com.aries.backend.storage.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 显式启用才访问真实 OSS；使用独立诊断对象并清理，不触及用户文章和素材记录。 */
@EnabledIfEnvironmentVariable(named = "ARIESHUB_OSS_LIVE_TEST", matches = "true")
class S3MultipartLiveTest {
    @Test
    void realOssAcknowledgesAllPartsAndObjectCanBeRemoved() {
        S3StorageProperties properties =
                new S3StorageProperties(
                        true,
                        URI.create(
                                System.getenv()
                                        .getOrDefault(
                                                "S3_ENDPOINT",
                                                "https://s3.oss-cn-hangzhou.aliyuncs.com")),
                        System.getenv().getOrDefault("S3_REGION", "aws-global"),
                        System.getenv().getOrDefault("S3_BUCKET", "aries-hub"),
                        System.getenv("S3_ACCESS_KEY_ID"),
                        System.getenv("S3_ACCESS_KEY_SECRET"),
                        false);
        S3StorageConfiguration configuration = new S3StorageConfiguration();
        try (S3Client client = configuration.s3Client(properties);
                S3Presigner presigner = configuration.s3Presigner(properties)) {
            S3ObjectStorage storage = new S3ObjectStorage(client, presigner, properties.bucket());
            String key = "diagnostics/upload-progress/" + UUID.randomUUID();
            byte[] bytes = new byte[13 * 1024 * 1024];
            List<Long> confirmed = new ArrayList<Long>();
            try {
                storage.put(
                        key,
                        new ByteArrayInputStream(bytes),
                        bytes.length,
                        org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE,
                        confirmed::add);
                assertThat(confirmed).contains(5L * 1024 * 1024, 10L * 1024 * 1024);
                assertThat(confirmed.getLast()).isEqualTo(bytes.length);
                assertThat(
                                client.headObject(
                                                software.amazon.awssdk.services.s3.model
                                                        .HeadObjectRequest.builder()
                                                        .bucket(properties.bucket())
                                                        .key(key)
                                                        .build())
                                        .contentLength())
                        .isEqualTo(bytes.length);
            } finally {
                storage.delete(key);
            }
        }
    }
}
