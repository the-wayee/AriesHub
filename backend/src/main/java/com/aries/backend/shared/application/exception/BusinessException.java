package com.aries.backend.shared.application.exception;

import lombok.Getter;

/** 应用用例的可预期失败；HTTP 状态映射由接口层统一处理。 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode code;
    private final Long retryAfterSeconds;

    public BusinessException(ErrorCode code) {
        this(code, null);
    }

    public BusinessException(ErrorCode code, Long retryAfterSeconds) {
        super(code.getMessage());
        this.retryAfterSeconds = retryAfterSeconds;
        this.code = code;
    }
}
