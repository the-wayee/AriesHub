package com.aries.backend.identity.infrastructure.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Resend 凭证只从运行环境读取，不写入源码或数据库。 */
@ConfigurationProperties("app.email.resend")
public record ResendEmailProperties(String apiKey, String from) {}
