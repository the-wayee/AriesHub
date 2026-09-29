package com.aries.backend.identity.application.service;

import com.aries.backend.identity.domain.model.Email;
import com.aries.backend.shared.application.exception.BusinessException;

import static com.aries.backend.identity.application.exception.IdentityErrorCode.INVALID_EMAIL;

/** 将外部邮箱输入转换为领域值对象，并保留稳定的客户端错误响应。 */
final class EmailInput {
    private EmailInput() {}

    static Email parse(String value) {
        try {
            return new Email(value);
        } catch (IllegalArgumentException error) {
            throw new BusinessException(INVALID_EMAIL);
        }
    }
}
