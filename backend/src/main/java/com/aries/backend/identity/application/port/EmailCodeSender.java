package com.aries.backend.identity.application.port;

import com.aries.backend.identity.domain.model.VerificationPurpose;

/** 邮件发送端口；应用层不依赖 Resend SDK 或 HTTP 客户端。 */
public interface EmailCodeSender {
    void send(String email, String code, VerificationPurpose purpose, String idempotencyKey);
}
