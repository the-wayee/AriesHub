package com.aries.backend.inspiration.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 全项目环境变量控制刷新周期和文案池容量，避免误配置冲击上游。 */
@ConfigurationProperties("app.inspiration")
public record PhilosophyQuoteProperties(boolean enabled, long refreshMs, int poolSize) {
    public PhilosophyQuoteProperties {
        if (refreshMs < 10_000 || poolSize < 1 || poolSize > 500)
            throw new IllegalArgumentException("文案刷新间隔至少 10000ms，缓存容量须为 1–500");
    }
}
