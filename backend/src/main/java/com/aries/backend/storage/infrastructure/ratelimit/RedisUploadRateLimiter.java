package com.aries.backend.storage.infrastructure.ratelimit;

import com.aries.backend.shared.application.exception.BusinessException;
import com.aries.backend.storage.application.port.UploadRateLimiter;
import com.aries.backend.storage.application.exception.StorageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.List;

/** 每用户每分钟最多 20 次上传，所有应用实例共享配额。 */
@Component
@RequiredArgsConstructor
public class RedisUploadRateLimiter implements UploadRateLimiter {
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            local count = tonumber(redis.call('GET', KEYS[1]) or '0')
            if count >= 20 then return math.max(1, math.ceil(redis.call('PTTL', KEYS[1]) / 1000)) end
            count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('PEXPIRE', KEYS[1], 60000) end
            return 0
            """, Long.class);
    @Override public void check(long userId) {
        Long retry = redis.execute(CONSUME, List.of("arieshub:storage:upload-rate:" + userId));
        if (retry == null) throw new IllegalStateException("Redis 未返回上传限流结果");
        if (retry > 0) throw new BusinessException(StorageErrorCode.UPLOAD_RATE_LIMITED, retry);
    }
}
