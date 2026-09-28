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
        USER_NOT_FOUND("登录账号不存在，请重新登录"),
        VERIFICATION_CODE_TOO_FREQUENT("验证码发送过于频繁，请稍后再试"),
        VERIFICATION_CODE_EXPIRED("验证码已过期，请重新获取"),
        VERIFICATION_CODE_INVALID("验证码不正确"),
        VERIFICATION_CODE_ATTEMPTS_EXCEEDED("验证码错误次数过多，请重新获取"),
        EMAIL_DELIVERY_FAILED("验证码邮件暂时无法发送，请稍后再试"),
        ADMIN_CASE_NOT_FOUND("案例不存在"),
        CATEGORY_NOT_FOUND("所选分类不存在"),
        CASE_SLUG_CONFLICT("案例地址已被使用");
        private final String message;
    }
}
