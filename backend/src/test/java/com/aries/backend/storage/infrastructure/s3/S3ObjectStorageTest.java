package com.aries.backend.storage.infrastructure.s3;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.List;

class S3ObjectStorageTest {
    @Test
    void multipartProgressCountsOnlyAcknowledgedPartsAndCompletesInOrder() {
        S3Client client = mock(S3Client.class);
        S3ObjectStorage storage =
                new S3ObjectStorage(client, mock(S3Presigner.class), "test-bucket");
        int partBytes = 5 * 1024 * 1024;
        byte[] bytes = new byte[partBytes + 123];
        List<Long> progress = new java.util.ArrayList<Long>();
        when(client.createMultipartUpload(
                        any(
                                software.amazon.awssdk.services.s3.model
                                        .CreateMultipartUploadRequest.class)))
                .thenReturn(
                        software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse
                                .builder()
                                .uploadId("upload-1")
                                .build());
        when(client.uploadPart(
                        any(software.amazon.awssdk.services.s3.model.UploadPartRequest.class),
                        any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenAnswer(
                        call -> {
                            UploadPartRequest request =
                                    (software.amazon.awssdk.services.s3.model.UploadPartRequest)
                                            call.getArgument(0);
                            // 下一片尚未得到 OSS 响应时，不得提前增加进度。
                            assertThat(progress.getLast())
                                    .isEqualTo(request.partNumber() == 1 ? 0L : partBytes);
                            return software.amazon.awssdk.services.s3.model.UploadPartResponse
                                    .builder()
                                    .eTag("etag-" + request.partNumber())
                                    .build();
                        });
        storage.put(
                "test-key",
                new java.io.ByteArrayInputStream(bytes),
                bytes.length,
                "video/mp4",
                progress::add);
        assertThat(progress)
                .containsExactly(0L, (long) partBytes, (long) partBytes, (long) bytes.length);
        ArgumentCaptor<CompleteMultipartUploadRequest> complete =
                org.mockito.ArgumentCaptor.forClass(
                        software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest
                                .class);
        verify(client).completeMultipartUpload(complete.capture());
        assertThat(complete.getValue().multipartUpload().parts())
                .extracting(p -> p.eTag())
                .containsExactly("etag-1", "etag-2");
        verify(client, never())
                .abortMultipartUpload(
                        any(
                                software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest
                                        .class));
    }

    @Test
    void failedPartDoesNotAdvanceProgressAndAbortsOssMultipart() {
        S3Client client = mock(S3Client.class);
        S3ObjectStorage storage =
                new S3ObjectStorage(client, mock(S3Presigner.class), "test-bucket");
        when(client.createMultipartUpload(
                        any(
                                software.amazon.awssdk.services.s3.model
                                        .CreateMultipartUploadRequest.class)))
                .thenReturn(
                        software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse
                                .builder()
                                .uploadId("upload-1")
                                .build());
        when(client.uploadPart(
                        any(software.amazon.awssdk.services.s3.model.UploadPartRequest.class),
                        any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenThrow(
                        software.amazon.awssdk.core.exception.SdkClientException.create(
                                "test failure"));
        List<Long> progress = new java.util.ArrayList<Long>();
        byte[] bytes = new byte[6 * 1024 * 1024];
        assertThatThrownBy(
                        () ->
                                storage.put(
                                        "test-key",
                                        new java.io.ByteArrayInputStream(bytes),
                                        bytes.length,
                                        "video/mp4",
                                        progress::add))
                .isInstanceOf(
                        com.aries.backend.shared.application.exception.BusinessException.class);
        assertThat(progress).containsExactly(0L);
        verify(client)
                .abortMultipartUpload(
                        any(
                                software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest
                                        .class));
        verify(client, never())
                .completeMultipartUpload(
                        any(
                                software.amazon.awssdk.services.s3.model
                                        .CompleteMultipartUploadRequest.class));
    }

    @Test
    void ossDownloadUsesVirtualHostAndV4Signature() {
        try (S3Presigner presigner =
                S3Presigner.builder()
                        .endpointOverride(URI.create("https://s3.oss-cn-hangzhou.aliyuncs.com"))
                        .region(Region.AWS_GLOBAL)
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create("test-key", "test-secret")))
                        .serviceConfiguration(
                                S3Configuration.builder()
                                        .pathStyleAccessEnabled(false)
                                        .chunkedEncodingEnabled(false)
                                        .build())
                        .build()) {
            S3ObjectStorage storage =
                    new S3ObjectStorage(mock(S3Client.class), presigner, "arieshub-test");
            URI url =
                    URI.create(
                            storage.downloadUrl(
                                    "uploads/42/attachment/file.pdf",
                                    "说明.pdf",
                                    Duration.ofMinutes(5)));
            assertThat(url.getHost()).isEqualTo("arieshub-test.s3.oss-cn-hangzhou.aliyuncs.com");
            assertThat(url.getPath()).isEqualTo("/uploads/42/attachment/file.pdf");
            assertThat(url.getQuery())
                    .contains(
                            "X-Amz-Algorithm=AWS4-HMAC-SHA256",
                            "X-Amz-Expires=300",
                            "response-content-disposition=attachment");
            URI avatar =
                    URI.create(
                            storage.imageUrl("uploads/42/avatar/image.png", Duration.ofMinutes(5)));
            assertThat(avatar.getQuery())
                    .contains("response-content-disposition=inline", "X-Amz-Expires=300");
        }
    }

    @Test
    void enabledConfigurationRequiresCredentialsAndPropertiesRedactSecrets() {
        S3StorageProperties config =
                new S3StorageProperties(
                        true,
                        URI.create("https://s3.oss-cn-hangzhou.aliyuncs.com"),
                        "aws-global",
                        "test-bucket",
                        "test-key",
                        "secret-value",
                        false);
        assertThat(config.isConfigurationValid()).isTrue();
        assertThat(config.toString()).doesNotContain("test-key", "secret-value");
        assertThat(
                        new S3StorageProperties(
                                        true,
                                        config.endpoint(),
                                        "aws-global",
                                        "test",
                                        "",
                                        "",
                                        false)
                                .isConfigurationValid())
                .isFalse();
    }
}
