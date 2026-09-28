package com.aries.backend.identity.infrastructure.verification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 注册验证码配置并在启动时校验范围。 */
@Configuration
@EnableConfigurationProperties(VerificationCodeProperties.class)
public class VerificationConfiguration {}
