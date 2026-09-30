package com.aries.backend.storage.infrastructure.s3;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.net.URI;

@Validated
@ConfigurationProperties("app.storage.s3")
public record S3StorageProperties(boolean enabled, URI endpoint, String region, String bucket,
                                  String accessKeyId, String accessKeySecret, boolean pathStyle) {
    @AssertTrue(message = "启用 S3 后必须配置 HTTPS endpoint、region、bucket 和访问凭据")
    public boolean isConfigurationValid() {
        return !enabled || (endpoint != null && "https".equals(endpoint.getScheme())
                && endpoint.getHost() != null && endpoint.getUserInfo() == null
                && endpoint.getQuery() == null && endpoint.getFragment() == null
                && (endpoint.getPath().isEmpty() || "/".equals(endpoint.getPath()))
                && present(region) && present(bucket) && present(accessKeyId) && present(accessKeySecret));
    }
    @Override public String toString() {
        return "S3StorageProperties[enabled=" + enabled + ", endpoint=" + endpoint
                + ", region=" + region + ", bucket=" + bucket + ", credentials=REDACTED]";
    }
    private boolean present(String value) { return value != null && !value.isBlank(); }
}
