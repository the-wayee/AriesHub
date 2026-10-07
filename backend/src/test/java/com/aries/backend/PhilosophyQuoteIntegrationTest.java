package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aries.backend.inspiration.application.port.PhilosophyQuoteProvider;
import com.aries.backend.inspiration.application.view.PhilosophyQuoteView;
import com.aries.backend.inspiration.infrastructure.PhilosophyQuoteProperties;
import com.aries.backend.inspiration.infrastructure.RedisPhilosophyQuoteCache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.util.concurrent.atomic.AtomicInteger;

/** 用真实 Redis 验证共享租约和重启恢复；测试不访问一言。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class PhilosophyQuoteIntegrationTest extends IntegrationTestSupport {
    @Test
    void anonymousColdPoolUsesResultAndDoesNotBlock() throws Exception {
        mvc.perform(get("/api/v1/inspiration/quote"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void instancesShareLeaseAndOldQuotesSurviveUpstreamFailureAndRestart() {
        AtomicInteger requests = new AtomicInteger();
        PhilosophyQuoteView quote = new PhilosophyQuoteView("来自真实缓存的句子", "测试出处");
        PhilosophyQuoteProvider provider =
                () -> {
                    if (requests.incrementAndGet() > 1) throw new IllegalStateException("模拟上游超时");
                    return quote;
                };
        PhilosophyQuoteProperties config = new PhilosophyQuoteProperties(true, 60_000, 60);
        RedisPhilosophyQuoteCache first =
                new RedisPhilosophyQuoteCache(redisTemplate, provider, config);
        RedisPhilosophyQuoteCache second =
                new RedisPhilosophyQuoteCache(redisTemplate, provider, config);
        first.refresh();
        second.refresh();
        assertThat(requests.get()).isEqualTo(1);
        assertThat(second.snapshot()).containsExactly(quote);
        assertThat(redisTemplate.getExpire("arieshub:inspiration:philosophy:pool")).isEqualTo(-1L);
        redisTemplate.delete("arieshub:inspiration:philosophy:refresh");
        second.refresh();
        assertThat(requests.get()).isEqualTo(2);
        assertThat(second.snapshot()).containsExactly(quote);
        RedisPhilosophyQuoteCache restarted =
                new RedisPhilosophyQuoteCache(redisTemplate, provider, config);
        restarted.refresh();
        assertThat(restarted.snapshot()).containsExactly(quote);
        assertThat(requests.get()).isEqualTo(2);
    }
}
