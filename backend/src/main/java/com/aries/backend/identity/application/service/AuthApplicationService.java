package com.aries.backend.identity.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.aries.backend.identity.application.port.RegistrationRolePolicy;
import com.aries.backend.identity.application.view.IdentityViews.CurrentUser;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import com.aries.backend.identity.domain.repository.UserRepository;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import static com.aries.backend.shared.application.exception.BusinessException.Code.*;

/** 注册、登录和当前用户查询用例。 */
@Service
@RequiredArgsConstructor
public class AuthApplicationService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService verification;
    private final RegistrationRolePolicy registrationRolePolicy;

    @Transactional
    public CurrentUser register(String email, String password, String nickname, String code) {
        String normalizedEmail = normalizeEmail(email);
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(EMAIL_ALREADY_REGISTERED);
        }
        verification.verify(normalizedEmail, VerificationPurpose.REGISTER, code);
        UserAccount pending = UserAccount.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .nickname(nickname.trim())
                .role(registrationRolePolicy.isAdmin(normalizedEmail)
                        ? UserAccount.Role.ADMIN : UserAccount.Role.USER)
                .status(UserAccount.Status.ACTIVE)
                .emailVerified(true)
                .build();
        try {
            UserAccount user = users.save(pending);
            StpUtil.login(user.getId());
            return CurrentUser.from(user);
        } catch (DuplicateKeyException error) {
            throw new BusinessException(EMAIL_ALREADY_REGISTERED);
        }
    }

    @Transactional
    public CurrentUser login(String email, String password, String code) {
        UserAccount user = users.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(INVALID_CREDENTIALS);
        }
        if (!user.canLogin()) {
            throw new BusinessException(ACCOUNT_DISABLED);
        }
        verification.verify(user.getEmail(), VerificationPurpose.LOGIN, code);
        users.updateLastLoginAt(user.getId(), OffsetDateTime.now(ZoneOffset.UTC));
        StpUtil.login(user.getId());
        return CurrentUser.from(user);
    }

    @Transactional(readOnly = true)
    public CurrentUser currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        if (!user.canLogin()) {
            StpUtil.logout();
            throw new BusinessException(ACCOUNT_DISABLED);
        }
        return CurrentUser.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
