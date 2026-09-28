package com.aries.backend.identity.domain.model;

/** 邮箱验证码用途必须参与 Redis key，避免注册验证码被用于登录。 */
public enum VerificationPurpose {
    REGISTER,
    LOGIN
}
