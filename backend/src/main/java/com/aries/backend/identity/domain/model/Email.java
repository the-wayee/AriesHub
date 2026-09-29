package com.aries.backend.identity.domain.model;

import java.util.Locale;

/** 规范化后的邮箱地址；身份模块只用这一种表示。 */
public record Email(String value) {
    public Email {
        if (value == null) throw new IllegalArgumentException("邮箱不能为空");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > 254 || !value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }
}
