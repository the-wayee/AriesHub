package com.aries.backend.identity.application.port;

import java.time.Duration;

/** 跨实例共享的原子限流计数器；不同用途使用独立的 scope。 */
public interface AuthRateCounter {
    boolean tryConsume(String scope, String subject, int limit, Duration window);
    void clear(String scope, String subject);
}
