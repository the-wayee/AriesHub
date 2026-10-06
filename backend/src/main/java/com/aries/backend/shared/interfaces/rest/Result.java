package com.aries.backend.shared.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

/** HTTP 返回契约；业务用例仍返回领域视图，追踪编号由请求过滤器生成。 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record Result<T>(String code, String msg, T data, String traceId) {
    public static <T> Result<T> success(T data) {
        return new Result<>("SUCCESS", "操作成功", data, MDC.get("traceId"));
    }

    public static <T> Result<T> failure(String code, String msg, T data, String traceId) {
        return new Result<>(code, msg, data, traceId);
    }
}
