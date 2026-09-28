package com.aries.backend.identity.infrastructure.verification;

import com.aries.backend.identity.application.port.VerificationCodeStore;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * Redis 验证码存储。
 *
 * <p>key 只包含邮箱 SHA-256，value 只保存 BCrypt 摘要，不在 Redis 中留下明文邮箱或验证码。</p>
 */
@Repository
@RequiredArgsConstructor
public class RedisVerificationCodeStore implements VerificationCodeStore {
    private static final String HASH_FIELD = "hash";
    private static final String ATTEMPTS_FIELD = "attempts";
    private static final DefaultRedisScript<Long> INCREMENT_IF_PRESENT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return -1 end
            return redis.call('HINCRBY', KEYS[1], 'attempts', 1)
            """, Long.class);
    private final StringRedisTemplate redis;

    @Override
    public boolean issue(String email, VerificationPurpose purpose, String codeHash,
                         Duration ttl, Duration cooldown) {
        String cooldownKey = cooldownKey(email, purpose);
        Boolean reserved = redis.opsForValue().setIfAbsent(cooldownKey, "1", cooldown);
        if (!Boolean.TRUE.equals(reserved)) return false;
        try {
            String codeKey = codeKey(email, purpose);
            redis.opsForHash().putAll(codeKey, Map.of(HASH_FIELD, codeHash, ATTEMPTS_FIELD, "0"));
            redis.expire(codeKey, ttl);
            redis.delete(consumedKey(email, purpose));
            return true;
        } catch (RuntimeException error) {
            redis.delete(cooldownKey);
            throw error;
        }
    }

    @Override
    public Optional<StoredCode> find(String email, VerificationPurpose purpose) {
        String key = codeKey(email, purpose);
        Object hash = redis.opsForHash().get(key, HASH_FIELD);
        Object attempts = redis.opsForHash().get(key, ATTEMPTS_FIELD);
        if (hash == null) return Optional.empty();
        long attemptCount = attempts == null ? 0 : Long.parseLong(attempts.toString());
        return Optional.of(new StoredCode(hash.toString(), attemptCount));
    }

    @Override
    public long incrementAttempts(String email, VerificationPurpose purpose) {
        Long attempts = redis.execute(INCREMENT_IF_PRESENT, java.util.List.of(codeKey(email, purpose)));
        return attempts == null || attempts < 0 ? Long.MAX_VALUE : attempts;
    }

    @Override
    public boolean consumeOnce(String email, VerificationPurpose purpose, Duration ttl) {
        Boolean first = redis.opsForValue().setIfAbsent(consumedKey(email, purpose), "1", ttl);
        if (Boolean.TRUE.equals(first)) redis.delete(codeKey(email, purpose));
        return Boolean.TRUE.equals(first);
    }

    @Override
    public void deleteCode(String email, VerificationPurpose purpose) {
        redis.delete(codeKey(email, purpose));
    }

    @Override
    public void rollbackIssue(String email, VerificationPurpose purpose) {
        redis.delete(codeKey(email, purpose));
        redis.delete(cooldownKey(email, purpose));
    }

    private String codeKey(String email, VerificationPurpose purpose) {
        return "arieshub:auth:code:" + purpose.name().toLowerCase() + ":" + emailHash(email);
    }

    private String cooldownKey(String email, VerificationPurpose purpose) {
        return "arieshub:auth:cooldown:" + purpose.name().toLowerCase() + ":" + emailHash(email);
    }

    private String consumedKey(String email, VerificationPurpose purpose) {
        return "arieshub:auth:consumed:" + purpose.name().toLowerCase() + ":" + emailHash(email);
    }

    private String emailHash(String email) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(email.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JDK 缺少 SHA-256", impossible);
        }
    }
}
