package com.aries.backend.identity.application.port;

import java.time.Duration;

/** 跨实例共享的原子限流计数器；不同用途使用独立的 scope。 */
public interface AuthRateCounter {
    /** 返回 0 表示放行，否则为当前窗口剩余秒数。 */
    long consumeRetryAfterSeconds(String scope, String subject, int limit, Duration window);
    void clear(String scope, String subject);
}
