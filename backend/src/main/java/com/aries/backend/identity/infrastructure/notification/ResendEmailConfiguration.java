package com.aries.backend.identity.infrastructure.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** 注册 Resend 外部服务配置。 */
@Configuration
@EnableConfigurationProperties(ResendEmailProperties.class)
public class ResendEmailConfiguration {
    /** Boot 当前依赖组合不自动创建 Builder，因此在邮件适配层提供可覆盖的默认实现。 */
    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder resendRestClientBuilder() {
        return RestClient.builder();
    }
}
