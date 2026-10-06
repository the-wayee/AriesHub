package com.aries.backend.storage.infrastructure.s3;

import com.aries.backend.shared.application.exception.BusinessException;
import com.aries.backend.storage.application.exception.StorageErrorCode;
import com.aries.backend.storage.application.port.ObjectStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.io.InputStream;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3StorageConfiguration {
    private S3Configuration options(S3StorageProperties p) {
        return S3Configuration.builder().pathStyleAccessEnabled(p.pathStyle())
                .chunkedEncodingEnabled(false).build();
    }
    private StaticCredentialsProvider credentials(S3StorageProperties p) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(p.accessKeyId(), p.accessKeySecret()));
    }
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.storage.s3.enabled", havingValue = "true")
    S3Client s3Client(S3StorageProperties p) {
        return S3Client.builder().endpointOverride(p.endpoint()).region(Region.of(p.region()))
                .credentialsProvider(credentials(p)).serviceConfiguration(options(p))
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(45))
                        .apiCallAttemptTimeout(Duration.ofSeconds(20)))
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED).build();
    }
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.storage.s3.enabled", havingValue = "true")
    S3Presigner s3Presigner(S3StorageProperties p) {
        return S3Presigner.builder().endpointOverride(p.endpoint()).region(Region.of(p.region()))
                .credentialsProvider(credentials(p)).serviceConfiguration(options(p)).build();
    }
    @Bean
    @ConditionalOnProperty(name = "app.storage.s3.enabled", havingValue = "true")
    ObjectStorage objectStorage(S3Client client, S3Presigner presigner, S3StorageProperties p) {
        return new S3ObjectStorage(client, presigner, p.bucket());
    }
    @Bean
    @ConditionalOnProperty(name = "app.storage.s3.enabled", havingValue = "false", matchIfMissing = true)
    ObjectStorage disabledObjectStorage() {
        return new ObjectStorage() {
            private BusinessException unavailable() { return new BusinessException(StorageErrorCode.STORAGE_UNAVAILABLE); }
            public void put(String key, InputStream content, long size, String type) { throw unavailable(); }
            public void delete(String key) { throw unavailable(); }
            public String downloadUrl(String key, String name, Duration ttl) { throw unavailable(); }
            public String imageUrl(String key, Duration ttl) { throw unavailable(); }
        };
    }
}
