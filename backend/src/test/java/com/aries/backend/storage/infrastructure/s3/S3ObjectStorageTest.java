package com.aries.backend.storage.infrastructure.s3;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.net.URI;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3ObjectStorageTest {
    @Test void ossDownloadUsesVirtualHostAndV4Signature() {
        try (var presigner = S3Presigner.builder().endpointOverride(URI.create("https://s3.oss-cn-hangzhou.aliyuncs.com"))
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(false).chunkedEncodingEnabled(false).build())
                .build()) {
            var storage = new S3ObjectStorage(mock(S3Client.class), presigner, "arieshub-test");
            URI url = URI.create(storage.downloadUrl("uploads/42/attachment/file.pdf", "说明.pdf", Duration.ofMinutes(5)));
            assertThat(url.getHost()).isEqualTo("arieshub-test.s3.oss-cn-hangzhou.aliyuncs.com");
            assertThat(url.getPath()).isEqualTo("/uploads/42/attachment/file.pdf");
            assertThat(url.getQuery()).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256", "X-Amz-Expires=300", "response-content-disposition=attachment");
        }
    }
    @Test void enabledConfigurationRequiresCredentialsAndPropertiesRedactSecrets() {
        var config = new S3StorageProperties(true, URI.create("https://s3.oss-cn-hangzhou.aliyuncs.com"),
                "aws-global", "test-bucket", "test-key", "secret-value", false);
        assertThat(config.isConfigurationValid()).isTrue();
        assertThat(config.toString()).doesNotContain("test-key", "secret-value");
        assertThat(new S3StorageProperties(true, config.endpoint(), "aws-global", "test", "", "", false)
                .isConfigurationValid()).isFalse();
    }
}
