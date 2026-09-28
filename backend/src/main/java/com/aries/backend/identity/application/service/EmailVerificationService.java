package com.aries.backend.identity.application.service;

import com.aries.backend.identity.application.port.EmailCodeSender;
import com.aries.backend.identity.application.port.VerificationCodeStore;
import com.aries.backend.identity.application.port.VerificationPolicy;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import com.aries.backend.identity.domain.repository.UserRepository;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

import static com.aries.backend.shared.application.exception.BusinessException.Code.*;

/** 发送与消费邮箱验证码；明文只在生成到交给 Resend 的短暂调用链中存在。 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final UserRepository users;
    private final VerificationCodeStore store;
    private final EmailCodeSender sender;
    private final VerificationPolicy properties;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public DispatchResult dispatch(String email, VerificationPurpose purpose) {
        String normalizedEmail = normalizeEmail(email);
        var existing = users.findByEmail(normalizedEmail);
        if (purpose == VerificationPurpose.REGISTER && existing.isPresent()) {
            throw new BusinessException(EMAIL_ALREADY_REGISTERED);
        }
        // 登录发码不暴露邮箱是否注册：不存在或停用时仍返回相同结果，但不调用外部邮件服务。
        if (purpose == VerificationPurpose.LOGIN &&
                (existing.isEmpty() || !existing.get().canLogin())) {
            return result();
        }

        String code = "%06d".formatted(random.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(code);
        if (!store.issue(normalizedEmail, purpose, codeHash,
                properties.ttl(), properties.resendCooldown())) {
            throw new BusinessException(VERIFICATION_CODE_TOO_FREQUENT);
        }
        try {
            sender.send(normalizedEmail, code, purpose,
                    "arieshub-code/" + purpose.name().toLowerCase() + "/" + UUID.randomUUID());
        } catch (RuntimeException error) {
            store.rollbackIssue(normalizedEmail, purpose);
            throw new BusinessException(EMAIL_DELIVERY_FAILED);
        }
        return result();
    }

    public void verify(String email, VerificationPurpose purpose, String code) {
        String normalizedEmail = normalizeEmail(email);
        VerificationCodeStore.StoredCode stored = store.find(normalizedEmail, purpose)
                .orElseThrow(() -> new BusinessException(VERIFICATION_CODE_EXPIRED));
        long attempts = store.incrementAttempts(normalizedEmail, purpose);
        if (attempts > properties.maxAttempts()) {
            store.deleteCode(normalizedEmail, purpose);
            throw new BusinessException(VERIFICATION_CODE_ATTEMPTS_EXCEEDED);
        }
        if (!passwordEncoder.matches(code, stored.hash())) {
            if (attempts >= properties.maxAttempts()) store.deleteCode(normalizedEmail, purpose);
            throw new BusinessException(VERIFICATION_CODE_INVALID);
        }
        if (!store.consumeOnce(normalizedEmail, purpose, properties.ttl())) {
            throw new BusinessException(VERIFICATION_CODE_INVALID);
        }
    }

    private DispatchResult result() {
        return new DispatchResult(properties.ttl().toSeconds(), properties.resendCooldown().toSeconds());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record DispatchResult(long expiresInSeconds, long resendAfterSeconds) {}
}
