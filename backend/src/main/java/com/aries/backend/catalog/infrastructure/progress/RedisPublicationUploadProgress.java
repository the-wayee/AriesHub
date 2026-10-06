package com.aries.backend.catalog.infrastructure.progress;

import com.aries.backend.catalog.application.exception.CatalogErrorCode;
import com.aries.backend.catalog.application.port.PublicationUploadProgress;
import com.aries.backend.shared.application.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Redis 让轮询与上传可落在不同实例；原子更新保留取消状态，任务十分钟自动过期。 */
@Component
@RequiredArgsConstructor
public class RedisPublicationUploadProgress implements PublicationUploadProgress {
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final DefaultRedisScript<Long> UPDATE =
            new DefaultRedisScript<>(
                    """
                    local current = redis.call('GET', KEYS[1])
                    if not current or string.sub(current, 1, 9) == 'CANCELLED' then return 0 end
                    redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
                    return 1
                    """,
                    Long.class);
    private final StringRedisTemplate redis;

    private String key(long owner, UUID id) {
        return "arieshub:catalog:upload:" + owner + ":" + id;
    }

    public void begin(long owner, UUID id, long total) {
        if (!Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(key(owner, id), "OSS:0:" + total, TTL)))
            throw new BusinessException(CatalogErrorCode.UPLOAD_TASK_UNAVAILABLE);
    }

    private void update(long owner, UUID id, String value) {
        Long result =
                redis.execute(
                        UPDATE, List.of(key(owner, id)), value, Long.toString(TTL.toSeconds()));
        if (result == null || result == 0)
            throw new BusinessException(CatalogErrorCode.UPLOAD_TASK_UNAVAILABLE);
    }

    public void confirmed(long owner, UUID id, long loaded, long total) {
        update(owner, id, (loaded == total ? "FINALIZING:" : "OSS:") + loaded + ":" + total);
    }

    public void completed(long owner, UUID id, long total) {
        update(owner, id, "COMPLETED:" + total + ":" + total);
    }

    public void failed(long owner, UUID id) {
        // 失败标记不得覆盖并发取消；原异常仍由上传接口的 Result 返回。
        redis.execute(
                UPDATE, List.of(key(owner, id)), "FAILED:0:0", Long.toString(TTL.toSeconds()));
    }

    public Status status(long owner, UUID id) {
        String value = redis.opsForValue().get(key(owner, id));
        if (value == null) return new Status("RECEIVING", 0, 0);
        String[] fields = value.split(":");
        return new Status(fields[0], Long.parseLong(fields[1]), Long.parseLong(fields[2]));
    }

    public void cancel(long owner, UUID id) {
        redis.opsForValue().set(key(owner, id), "CANCELLED:0:0", TTL);
    }
}
