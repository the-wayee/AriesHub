package com.aries.backend.inspiration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aries.backend.inspiration.application.port.PhilosophyQuoteProvider;
import com.aries.backend.inspiration.application.service.PhilosophyQuoteService;
import com.aries.backend.inspiration.application.view.PhilosophyQuoteView;
import com.aries.backend.inspiration.infrastructure.PhilosophyQuoteProperties;
import com.aries.backend.inspiration.infrastructure.RedisPhilosophyQuoteCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 用户流量、上游失败、跨实例租约及有界去重是缓存的重要行为边界。 */
class PhilosophyQuoteCacheTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);

    private final PhilosophyQuoteProvider provider = mock(PhilosophyQuoteProvider.class);
    private final Map<String, String> stored = new HashMap<>();
    private final PhilosophyQuoteView first = new PhilosophyQuoteView("缓存中的哲学文案", "作者 · 出处");

    @BeforeEach
    void setup() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> stored.get(call.getArgument(0)));
        doAnswer(
                        call -> {
                            stored.put(call.getArgument(0), call.getArgument(1));
                            return null;
                        })
                .when(values)
                .set(anyString(), anyString());
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    private RedisPhilosophyQuoteCache cache(int size) {
        return new RedisPhilosophyQuoteCache(
                redis, provider, new PhilosophyQuoteProperties(true, 60_000, size));
    }

    @Test
    void userTrafficReadsOnlyImmutableMemorySnapshot() {
        when(provider.fetch()).thenReturn(first);
        RedisPhilosophyQuoteCache cache = cache(60);
        cache.refresh();
        PhilosophyQuoteService service = new PhilosophyQuoteService(cache);
        clearInvocations(redis, values, provider);
        for (int count = 0; count < 10_000; count++) assertThat(service.random()).isEqualTo(first);
        verifyNoInteractions(redis, values, provider);
    }

    @Test
    void upstreamAndRedisFailuresKeepLastGoodSnapshot() {
        when(provider.fetch()).thenReturn(first).thenThrow(new IllegalStateException("上游超时"));
        RedisPhilosophyQuoteCache cache = cache(60);
        cache.refresh();
        cache.refresh();
        assertThat(cache.snapshot()).containsExactly(first);
        when(values.get(anyString())).thenThrow(new IllegalStateException("Redis 暂不可用"));
        cache.refresh();
        assertThat(cache.snapshot()).containsExactly(first);
    }

    @Test
    void otherInstanceLoadsPersistedPoolWithoutBypassingRefreshLease() {
        when(provider.fetch()).thenReturn(first);
        cache(60).refresh();
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        clearInvocations(provider);
        RedisPhilosophyQuoteCache restarted = cache(60);
        restarted.refresh();
        assertThat(restarted.snapshot()).containsExactly(first);
        verifyNoInteractions(provider);
    }

    @Test
    void poolIsBoundedAndDuplicateTextDoesNotOccupyAnotherSlot() {
        PhilosophyQuoteView second = new PhilosophyQuoteView("第二条", null);
        PhilosophyQuoteView third = new PhilosophyQuoteView("第三条", null);
        when(provider.fetch()).thenReturn(first, second, third, third);
        RedisPhilosophyQuoteCache cache = cache(2);
        for (int count = 0; count < 4; count++) cache.refresh();
        assertThat(cache.snapshot()).isEqualTo(List.of(second, third));
    }

    @Test
    void emptyPoolDoesNotMakeSynchronousExternalRequests() {
        assertThat(new PhilosophyQuoteService(cache(60)).random()).isNull();
        verifyNoInteractions(redis, values, provider);
    }
}
