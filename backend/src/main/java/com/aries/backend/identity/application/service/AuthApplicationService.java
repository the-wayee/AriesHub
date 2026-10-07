package com.aries.backend.identity.application.service;

import static com.aries.backend.identity.application.exception.IdentityErrorCode.ACCOUNT_DISABLED;
import static com.aries.backend.identity.application.exception.IdentityErrorCode.EMAIL_ALREADY_REGISTERED;
import static com.aries.backend.identity.application.exception.IdentityErrorCode.INVALID_CREDENTIALS;

import com.aries.backend.identity.application.event.MemberJoined;
import com.aries.backend.identity.application.port.RegistrationRolePolicy;
import com.aries.backend.identity.application.port.SessionManager;
import com.aries.backend.identity.application.view.IdentityViews.CurrentUser;
import com.aries.backend.identity.domain.model.Email;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import com.aries.backend.identity.domain.repository.UserRepository;
import com.aries.backend.shared.application.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 注册、登录和退出用例。 */
@Service
@RequiredArgsConstructor
public class AuthApplicationService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService verification;
    private final RegistrationRolePolicy registrationRolePolicy;
    private final SessionManager sessions;
    private final AuthTrafficGuard trafficGuard;
    private final ApplicationEventPublisher events;

    @Transactional
    public CurrentUser register(String email, String password, String nickname, String code) {
        Email normalizedEmail = EmailInput.parse(email);
        trafficGuard.checkRegisterEmail(normalizedEmail);
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(EMAIL_ALREADY_REGISTERED);
        }
        verification.verify(normalizedEmail, VerificationPurpose.REGISTER, code);
        UserAccount pending =
                UserAccount.builder()
                        .email(normalizedEmail)
                        .passwordHash(passwordEncoder.encode(password))
                        .nickname(nickname.trim())
                        .role(
                                registrationRolePolicy.isAdmin(normalizedEmail)
                                        ? UserAccount.Role.ADMIN
                                        : UserAccount.Role.USER)
                        .status(UserAccount.Status.ACTIVE)
                        .emailVerified(true)
                        .build();
        try {
            UserAccount user = users.save(pending);
            sessions.login(user.getId());
            events.publishEvent(new MemberJoined(user.getId()));
            return CurrentUser.from(user);
        } catch (DuplicateKeyException error) {
            throw new BusinessException(EMAIL_ALREADY_REGISTERED);
        }
    }

    @Transactional
    public CurrentUser login(String email, String password) {
        Email normalizedEmail = EmailInput.parse(email);
        trafficGuard.checkLoginEmail(normalizedEmail);
        UserAccount user =
                users.findByEmail(normalizedEmail)
                        .orElseThrow(() -> new BusinessException(INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(INVALID_CREDENTIALS);
        }
        if (!user.canLogin()) {
            throw new BusinessException(ACCOUNT_DISABLED);
        }
        users.updateLastLoginAt(user.getId(), OffsetDateTime.now(ZoneOffset.UTC));
        sessions.login(user.getId());
        trafficGuard.loginSucceeded(normalizedEmail);
        return CurrentUser.from(user);
    }

    public void logout() {
        sessions.logout();
    }
}
