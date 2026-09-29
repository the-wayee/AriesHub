package com.aries.backend.identity.infrastructure.security;

import com.aries.backend.identity.application.port.AuthRateCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/** Redis 原子计数；key 仅保存邮箱或来源地址的摘要，并在窗口结束后自动过期。 */
@Repository
@RequiredArgsConstructor
public class RedisAuthRateCounter implements AuthRateCounter {
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current >= tonumber(ARGV[1]) then return 0 end
            current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end
            return 1
            """, Long.class);
    private final StringRedisTemplate redis;

    @Override
    public boolean tryConsume(String scope, String subject, int limit, Duration window) {
        Long allowed = redis.execute(CONSUME, List.of(key(scope, subject)),
                String.valueOf(limit), String.valueOf(window.toMillis()));
        return Long.valueOf(1).equals(allowed);
    }

    @Override
    public void clear(String scope, String subject) {
        redis.delete(key(scope, subject));
    }

    private String key(String scope, String subject) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(subject.getBytes(StandardCharsets.UTF_8));
            return "arieshub:auth:rate:" + scope + ":" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JDK 缺少 SHA-256", impossible);
        }
    }
}
