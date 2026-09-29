package com.aries.backend.identity.infrastructure.security;

import com.aries.backend.identity.application.port.RegistrationRolePolicy;
import com.aries.backend.identity.domain.model.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/** 根据运行环境白名单为新注册账号授予管理员角色。 */
@Component
@RequiredArgsConstructor
public class AdminBootstrapPolicy implements RegistrationRolePolicy {
    private final AdminEmailProperties properties;

    @Override
    public boolean isAdmin(Email email) {
        return properties.adminEmails().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(Email::new)
                .anyMatch(email::equals);
    }
}
