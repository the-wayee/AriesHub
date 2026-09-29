package com.aries.backend.shared.application.exception;

/** 模块自有错误码契约；错误类别由接口层转换为 HTTP 状态。 */
public interface ErrorCode {
    String name();
    String getMessage();
    Kind getKind();

    enum Kind {
        BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT,
        TOO_MANY_REQUESTS, SERVICE_UNAVAILABLE
    }
}
