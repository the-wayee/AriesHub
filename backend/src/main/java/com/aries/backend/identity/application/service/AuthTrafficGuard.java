package com.aries.backend.identity.application.service;

import com.aries.backend.identity.application.port.AuthRateCounter;
import com.aries.backend.identity.domain.model.Email;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.aries.backend.identity.application.exception.IdentityErrorCode.AUTH_RATE_LIMITED;

/** 登录、注册及注册发码的独立来源与邮箱限额。 */
@Service
@RequiredArgsConstructor
public class AuthTrafficGuard {
    private final AuthRateCounter counter;

    public void checkLoginSource(String address) {
        require("login-ip", address, 60, Duration.ofMinutes(10));
    }

    public void checkRegisterSource(String address) {
        require("register-ip", address, 30, Duration.ofHours(1));
    }

    public void checkCodeSource(String address) {
        require("code-ip", address, 60, Duration.ofHours(1));
    }

    public void checkLoginEmail(Email email) {
        require("login-email", email.value(), 5, Duration.ofMinutes(15));
    }

    public void loginSucceeded(Email email) {
        counter.clear("login-email", email.value());
    }

    public void checkRegisterEmail(Email email) {
        require("register-email", email.value(), 5, Duration.ofMinutes(15));
    }

    public void checkCodeEmail(Email email) {
        require("code-email", email.value(), 5, Duration.ofHours(1));
    }

    private void require(String scope, String subject, int limit, Duration window) {
        long retryAfter = counter.consumeRetryAfterSeconds(scope, subject, limit, window);
        if (retryAfter > 0) {
            throw new BusinessException(AUTH_RATE_LIMITED, retryAfter);
        }
    }
}
