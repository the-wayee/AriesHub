package com.aries.backend.identity.infrastructure.security;

import com.aries.backend.identity.application.port.RegistrationRolePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** 根据运行环境白名单为新注册账号授予管理员角色。 */
@Component
@RequiredArgsConstructor
public class AdminBootstrapPolicy implements RegistrationRolePolicy {
    private final AdminEmailProperties properties;

    @Override
    public boolean isAdmin(String normalizedEmail) {
        return properties.adminEmails().stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(normalizedEmail::equals);
    }
}
