package com.aries.backend.inspiration.infrastructure;

import com.aries.backend.inspiration.application.port.PhilosophyQuotePool;
import com.aries.backend.inspiration.application.port.PhilosophyQuoteProvider;
import com.aries.backend.inspiration.application.view.PhilosophyQuoteView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Redis 留存文案池，各实例保留内存快照；失败不清空，读取不访问 Redis 或一言。 */
@Component
public class RedisPhilosophyQuoteCache implements PhilosophyQuotePool {
    private static final String POOL_KEY = "arieshub:inspiration:philosophy:pool";
    private static final String REFRESH_KEY = "arieshub:inspiration:philosophy:refresh";
    private static final Logger LOG = LoggerFactory.getLogger(RedisPhilosophyQuoteCache.class);
    private final StringRedisTemplate redis;
    private final PhilosophyQuoteProvider provider;
    private final PhilosophyQuoteProperties properties;
    private final JsonMapper json = JsonMapper.builder().build();
    private volatile List<PhilosophyQuoteView> quotes = List.of();

    public RedisPhilosophyQuoteCache(
            StringRedisTemplate redis,
            PhilosophyQuoteProvider provider,
            PhilosophyQuoteProperties properties) {
        this.redis = redis;
        this.provider = provider;
        this.properties = properties;
    }

    @Override
    public List<PhilosophyQuoteView> snapshot() {
        return quotes;
    }

    @Scheduled(fixedDelayString = "${app.inspiration.refresh-ms:60000}", initialDelay = 1000)
    public void refresh() {
        if (!properties.enabled()) return;
        try {
            loadSharedSnapshot();
            // 租约保留至周期结束，不主动删除；多个实例和连续失败不会放大上游请求。
            if (!Boolean.TRUE.equals(
                    redis.opsForValue()
                            .setIfAbsent(
                                    REFRESH_KEY, "1", Duration.ofMillis(properties.refreshMs()))))
                return;
            PhilosophyQuoteView next = provider.fetch();
            if (next == null) return;
            List<PhilosophyQuoteView> updated = new ArrayList<>(quotes);
            updated.removeIf(existing -> existing.text().equals(next.text()));
            updated.add(next);
            if (updated.size() > properties.poolSize())
                updated =
                        new ArrayList<>(
                                updated.subList(
                                        updated.size() - properties.poolSize(), updated.size()));
            // 整体写入是原子的；不设置过期时间，中断或重启仍能使用最后一次有效池。
            redis.opsForValue().set(POOL_KEY, json.writeValueAsString(updated));
            quotes = List.copyOf(updated);
        } catch (Exception exception) {
            LOG.warn("哲学文案缓存刷新失败，继续使用 {} 条已有文案", quotes.size());
        }
    }

    private void loadSharedSnapshot() {
        String stored = redis.opsForValue().get(POOL_KEY);
        if (stored == null) return;
        List<PhilosophyQuoteView> valid =
                Arrays.stream(json.readValue(stored, PhilosophyQuoteView[].class))
                        .filter(
                                quote ->
                                        quote != null
                                                && quote.text() != null
                                                && !quote.text().isBlank())
                        .toList();
        if (!valid.isEmpty()) quotes = List.copyOf(valid);
    }
}
