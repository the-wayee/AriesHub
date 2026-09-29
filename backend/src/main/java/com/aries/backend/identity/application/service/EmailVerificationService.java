package com.aries.backend.identity.application.service;

import com.aries.backend.identity.application.port.EmailCodeSender;
import com.aries.backend.identity.application.port.VerificationCodeStore;
import com.aries.backend.identity.application.port.VerificationPolicy;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import com.aries.backend.identity.domain.model.Email;
import com.aries.backend.identity.domain.repository.UserRepository;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.aries.backend.identity.application.exception.IdentityErrorCode.*;

/** 发送与消费邮箱验证码；明文只在生成到交给 Resend 的短暂调用链中存在。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    private static final Pattern PROVIDER_ERROR_NAME =
            Pattern.compile("\"name\"\\s*:\\s*\"([A-Za-z0-9_]+)\"");
    private final UserRepository users;
    private final VerificationCodeStore store;
    private final EmailCodeSender sender;
    private final VerificationPolicy properties;
    private final PasswordEncoder passwordEncoder;
    private final AuthTrafficGuard trafficGuard;
    private final SecureRandom random = new SecureRandom();

    public DispatchResult dispatch(String email, VerificationPurpose purpose) {
        Email normalizedEmail = EmailInput.parse(email);
        trafficGuard.checkCodeEmail(normalizedEmail);
        var existing = users.findByEmail(normalizedEmail);
        if (purpose == VerificationPurpose.REGISTER && existing.isPresent()) {
            throw new BusinessException(EMAIL_ALREADY_REGISTERED);
        }
        String code = "%06d".formatted(random.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(code);
        if (!store.issue(normalizedEmail.value(), purpose, codeHash,
                properties.ttl(), properties.resendCooldown())) {
            throw new BusinessException(VERIFICATION_CODE_TOO_FREQUENT);
        }
        try {
            sender.send(normalizedEmail.value(), code, purpose,
                    "arieshub-code/" + purpose.name().toLowerCase() + "/" + UUID.randomUUID());
        } catch (RuntimeException error) {
            store.rollbackIssue(normalizedEmail.value(), purpose);
            logDeliveryFailure(error);
            throw new BusinessException(EMAIL_DELIVERY_FAILED);
        }
        return result();
    }

    public void verify(Email email, VerificationPurpose purpose, String code) {
        VerificationCodeStore.StoredCode stored = store.find(email.value(), purpose)
                .orElseThrow(() -> new BusinessException(VERIFICATION_CODE_EXPIRED));
        long attempts = store.incrementAttempts(email.value(), purpose);
        if (attempts > properties.maxAttempts()) {
            store.deleteCode(email.value(), purpose);
            throw new BusinessException(VERIFICATION_CODE_ATTEMPTS_EXCEEDED);
        }
        if (!passwordEncoder.matches(code, stored.hash())) {
            if (attempts >= properties.maxAttempts()) store.deleteCode(email.value(), purpose);
            throw new BusinessException(VERIFICATION_CODE_INVALID);
        }
        if (!store.consumeOnce(email.value(), purpose, properties.ttl())) {
            throw new BusinessException(VERIFICATION_CODE_INVALID);
        }
    }

    private DispatchResult result() {
        return new DispatchResult(properties.ttl().toSeconds(), properties.resendCooldown().toSeconds());
    }

    private void logDeliveryFailure(RuntimeException error) {
        if (error instanceof RestClientResponseException response) {
            var matcher = PROVIDER_ERROR_NAME.matcher(response.getResponseBodyAsString());
            String providerError = matcher.find() ? matcher.group(1) : "unknown";
            log.warn("验证码邮件投递失败，requestId={}，providerStatus={}，providerError={}",
                    MDC.get("requestId"), response.getStatusCode().value(), providerError);
        } else {
            log.warn("验证码邮件投递失败，requestId={}，cause={}",
                    MDC.get("requestId"), error.getClass().getSimpleName());
        }
    }

    public record DispatchResult(long expiresInSeconds, long resendAfterSeconds) {}
}
