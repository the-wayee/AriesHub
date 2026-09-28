package com.aries.backend.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** 首批管理员邮箱白名单，通过环境变量注入。 */
@ConfigurationProperties("app.security")
public record AdminEmailProperties(List<String> adminEmails) {
    public AdminEmailProperties {
        adminEmails = adminEmails == null ? List.of() : List.copyOf(adminEmails);
    }
}
