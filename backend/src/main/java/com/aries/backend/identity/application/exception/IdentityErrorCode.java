package com.aries.backend.identity.application.exception;

import com.aries.backend.shared.application.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 身份模块的稳定业务错误码。 */
@Getter
@RequiredArgsConstructor
public enum IdentityErrorCode implements ErrorCode {
    INVALID_EMAIL("邮箱格式不正确", Kind.BAD_REQUEST),
    INVALID_PROFILE("昵称需为 2–30 字，个性签名最多 160 字", Kind.BAD_REQUEST),
    AVATAR_NOT_SET("尚未设置头像", Kind.NOT_FOUND),
    INVALID_AVATAR("头像仅支持 PNG、JPEG、WebP，最大 5 MiB", Kind.BAD_REQUEST),
    AVATAR_FILE_NOT_FOUND("头像不存在或无权使用", Kind.NOT_FOUND),
    EMAIL_ALREADY_REGISTERED("该邮箱已注册，请直接登录", Kind.CONFLICT),
    INVALID_CREDENTIALS("邮箱或密码不正确", Kind.UNAUTHORIZED),
    AUTH_RATE_LIMITED("操作过于频繁，请稍后再试", Kind.TOO_MANY_REQUESTS),
    ACCOUNT_DISABLED("账号已被停用", Kind.FORBIDDEN),
    ADMIN_USER_NOT_FOUND("成员不存在", Kind.NOT_FOUND),
    ADMIN_USER_PROTECTED("不能停用管理员账号", Kind.FORBIDDEN),
    USER_NOT_FOUND("登录账号不存在，请重新登录", Kind.UNAUTHORIZED),
    VERIFICATION_CODE_TOO_FREQUENT("验证码发送过于频繁，请稍后再试", Kind.TOO_MANY_REQUESTS),
    VERIFICATION_CODE_EXPIRED("验证码已过期，请重新获取", Kind.BAD_REQUEST),
    VERIFICATION_CODE_INVALID("验证码不正确", Kind.BAD_REQUEST),
    VERIFICATION_CODE_ATTEMPTS_EXCEEDED("验证码错误次数过多，请重新获取", Kind.BAD_REQUEST),
    EMAIL_DELIVERY_FAILED("验证码邮件暂时无法发送，请稍后再试", Kind.SERVICE_UNAVAILABLE);

    private final String message;
    private final Kind kind;
}
