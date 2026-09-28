package com.aries.backend.identity.application.port;

import java.time.Duration;

/** 向验证码用例提供有效期、重发冷却和错误次数限制。 */
public interface VerificationPolicy {
    Duration ttl();

    Duration resendCooldown();

    int maxAttempts();
}
