package com.aries.backend.identity.interfaces.rest.request;

import com.aries.backend.identity.domain.model.VerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 申请邮箱验证码，purpose 用于隔离注册和登录凭证。 */
public record EmailCodeRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull VerificationPurpose purpose
) {}
