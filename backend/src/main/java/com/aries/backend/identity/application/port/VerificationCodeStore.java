package com.aries.backend.identity.application.port;

import com.aries.backend.identity.domain.model.VerificationPurpose;

import java.time.Duration;
import java.util.Optional;

/** 验证码临时存储端口；实现必须支持过期、限频、计数和一次性消费。 */
public interface VerificationCodeStore {
    record StoredCode(String hash, long attempts) {}

    boolean issue(String email, VerificationPurpose purpose, String codeHash,
                  Duration ttl, Duration cooldown);

    Optional<StoredCode> find(String email, VerificationPurpose purpose);

    long incrementAttempts(String email, VerificationPurpose purpose);

    boolean consumeOnce(String email, VerificationPurpose purpose, Duration ttl);

    void deleteCode(String email, VerificationPurpose purpose);

    void rollbackIssue(String email, VerificationPurpose purpose);
}
