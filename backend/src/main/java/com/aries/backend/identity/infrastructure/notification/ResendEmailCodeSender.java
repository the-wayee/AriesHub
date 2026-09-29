package com.aries.backend.identity.infrastructure.notification;

import com.aries.backend.identity.application.port.EmailCodeSender;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

/** 通过 Resend 官方 HTTP API 发送真实邮箱验证码。 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend", matchIfMissing = true)
public class ResendEmailCodeSender implements EmailCodeSender {
    private final ResendEmailProperties properties;
    private final RestClient client;

    public ResendEmailCodeSender(ResendEmailProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.client = builder.baseUrl("https://api.resend.com").build();
    }

    @Override
    public void send(String email, String code, VerificationPurpose purpose, String idempotencyKey) {
        if (!StringUtils.hasText(properties.apiKey()) || !StringUtils.hasText(properties.from())) {
            throw new IllegalStateException("Resend API Key 或发件地址未配置");
        }
        String action = "注册";
        client.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .header("Idempotency-Key", idempotencyKey)
                .body(new SendEmailRequest(
                        properties.from(),
                        List.of(email),
                        "AriesHub " + action + "验证码",
                        html(code, action),
                        "你的 AriesHub " + action + "验证码是 " + code + "，10 分钟内有效。"
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private String html(String code, String action) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:32px;color:#262d28">
                  <p style="font-size:12px;letter-spacing:2px;color:#657066">ARIESHUB / EMAIL CODE</p>
                  <h1 style="font-size:24px">%s验证码</h1>
                  <p>你正在进行 AriesHub %s，本次验证码为：</p>
                  <p style="font-size:36px;font-weight:700;letter-spacing:8px;margin:28px 0">%s</p>
                  <p style="color:#657066">验证码 10 分钟内有效，请勿转发给任何人。如果不是你本人操作，可以忽略这封邮件。</p>
                </div>
                """.formatted(action, action, code);
    }

    private record SendEmailRequest(
            String from,
            List<String> to,
            String subject,
            String html,
            String text
    ) {}
}
