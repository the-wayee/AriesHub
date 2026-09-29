package com.aries.backend;

import com.aries.backend.identity.application.port.EmailCodeSender;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration(proxyBeanMethods = false)
class TestEmailConfiguration {
    @Bean
    @Primary
    FakeEmailCodeSender fakeEmailCodeSender() {
        return new FakeEmailCodeSender();
    }
}

class FakeEmailCodeSender implements EmailCodeSender {
    private final Map<String, String> codes = new ConcurrentHashMap<>();

    @Override
    public void send(String email, String code, VerificationPurpose purpose, String idempotencyKey) {
        codes.put(key(email, purpose), code);
    }

    String latest(String email, VerificationPurpose purpose) {
        return codes.get(key(email, purpose));
    }

    void clear() {
        codes.clear();
    }

    private String key(String email, VerificationPurpose purpose) {
        return purpose.name() + ":" + email;
    }
}
