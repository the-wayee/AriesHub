package com.aries.backend.identity.infrastructure.verification;

import com.aries.backend.identity.application.port.VerificationPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** 邮箱验证码的有效期、发送间隔和最大尝试次数。 */
@Validated
@ConfigurationProperties("app.verification-code")
public record VerificationCodeProperties(
        @NotNull Duration ttl,
        @NotNull Duration resendCooldown,
        @Min(1) @Max(20) int maxAttempts
) implements VerificationPolicy {}
