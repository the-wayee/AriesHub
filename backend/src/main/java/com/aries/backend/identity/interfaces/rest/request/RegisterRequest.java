package com.aries.backend.identity.interfaces.rest.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 注册参数；密码至少包含一个英文字母和一个数字。 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$") String password,
        @NotBlank @Size(min = 2, max = 30) String nickname,
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {}
