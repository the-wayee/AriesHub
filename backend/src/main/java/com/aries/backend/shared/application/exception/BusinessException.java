package com.aries.backend.shared.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 应用用例的可预期失败；HTTP 状态映射由接口层统一处理。 */
@Getter
public class BusinessException extends RuntimeException {
    private final Code code;

    public BusinessException(Code code) {
        super(code.getMessage());
        this.code = code;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Code {
        CASE_NOT_FOUND("案例不存在或尚未公开"),
        CONTENT_LOCKED("该案例的完整内容暂未开放访问"),
        EMAIL_ALREADY_REGISTERED("该邮箱已注册，请直接登录"),
        INVALID_CREDENTIALS("邮箱或密码不正确"),
        ACCOUNT_DISABLED("账号已被停用"),
        USER_NOT_FOUND("登录账号不存在，请重新登录");
        private final String message;
    }
}
