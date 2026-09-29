package com.aries.backend.identity.domain.model;

/** 当前只有注册需要邮箱验证码；用途仍参与 Redis key，便于未来扩展时隔离凭证。 */
public enum VerificationPurpose {
    REGISTER
}
