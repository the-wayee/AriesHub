package com.aries.backend.shared.interfaces.rest;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;

import com.aries.backend.shared.application.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 统一错误响应：仅返回稳定错误码和追踪编号，内部异常详情仅记录到服务端。 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {
    public record RetryAfter(long retryAfterSeconds) {}

    @ExceptionHandler(BusinessException.class)
    Result<?> business(
            BusinessException error, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status =
                switch (error.getCode().getKind()) {
                    case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
                    case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
                    case FORBIDDEN -> HttpStatus.FORBIDDEN;
                    case NOT_FOUND -> HttpStatus.NOT_FOUND;
                    case CONFLICT -> HttpStatus.CONFLICT;
                    case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
                    case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
                };
        response.setStatus(status.value());
        if (error.getRetryAfterSeconds() != null) {
            response.setHeader("Retry-After", error.getRetryAfterSeconds().toString());
        }
        return Result.failure(
                error.getCode().name(),
                error.getMessage(),
                error.getRetryAfterSeconds() == null
                        ? null
                        : new RetryAfter(error.getRetryAfterSeconds()),
                (String) request.getAttribute(RequestIdFilter.ATTRIBUTE));
    }

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    Result<?> notLoggedIn(Exception error, HttpServletRequest request) {
        return failure("UNAUTHENTICATED", "请先登录后再访问", request);
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Result<?> forbidden(Exception error, HttpServletRequest request) {
        return failure("FORBIDDEN", "当前账号无权执行此操作", request);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        ConstraintViolationException.class,
        MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Result<?> invalid(Exception error, HttpServletRequest request) {
        return failure("INVALID_REQUEST", "请求参数不正确，请检查筛选条件或页码", request);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    Result<?> oversized(Exception error, HttpServletRequest request) {
        return failure("FILE_TOO_LARGE", "文件超过上传大小限制（最大 100MB）", request);
    }

    @ExceptionHandler(
            org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Result<?> missingFile(Exception error, HttpServletRequest request) {
        return failure("INVALID_REQUEST", "请提供要上传的文件", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Result<?> notFound(Exception error, HttpServletRequest request) {
        return failure("NOT_FOUND", "请求的资源不存在", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    Result<?> method(Exception error, HttpServletRequest request) {
        return failure("METHOD_NOT_ALLOWED", "此接口不支持该请求方式", request);
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    Result<?> database(Exception error, HttpServletRequest request) {
        log.error("数据库请求失败，traceId={}", request.getAttribute(RequestIdFilter.ATTRIBUTE), error);
        return failure("SERVICE_UNAVAILABLE", "内容服务暂时不可用，请稍后重试", request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Result<?> unexpected(Exception error, HttpServletRequest request) {
        log.error("请求发生未预期错误，traceId={}", request.getAttribute(RequestIdFilter.ATTRIBUTE), error);
        return failure("INTERNAL_ERROR", "服务暂时无法处理请求，请稍后重试", request);
    }

    private Result<Void> failure(String code, String message, HttpServletRequest request) {
        return Result.failure(
                code, message, null, (String) request.getAttribute(RequestIdFilter.ATTRIBUTE));
    }
}
