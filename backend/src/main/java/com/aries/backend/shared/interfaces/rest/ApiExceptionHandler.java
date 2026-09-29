package com.aries.backend.shared.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

/** 统一错误响应：仅返回稳定错误码和追踪编号，内部异常详情仅记录到服务端。 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErrorBody(String code, String message, String requestId) {}

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorBody> business(BusinessException error, HttpServletRequest request) {
        HttpStatus status = switch (error.getCode().getKind()) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return response(status, error.getCode().name(), error.getMessage(), request);
    }

    @ExceptionHandler(NotLoginException.class)
    ResponseEntity<ErrorBody> notLoggedIn(Exception error, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "请先登录后再访问", request);
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    ResponseEntity<ErrorBody> forbidden(Exception error, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "当前账号无权执行此操作", request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ErrorBody> invalid(Exception error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "请求参数不正确，请检查筛选条件或页码", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorBody> notFound(Exception error, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "请求的资源不存在", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorBody> method(Exception error, HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "此接口不支持该请求方式", request);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorBody> database(Exception error, HttpServletRequest request) {
        log.error("数据库请求失败，requestId={}", request.getAttribute(RequestIdFilter.ATTRIBUTE), error);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "内容服务暂时不可用，请稍后重试", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> unexpected(Exception error, HttpServletRequest request) {
        log.error("请求发生未预期错误，requestId={}", request.getAttribute(RequestIdFilter.ATTRIBUTE), error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务暂时无法处理请求，请稍后重试", request);
    }

    private ResponseEntity<ErrorBody> response(HttpStatus status, String code, String message,
                                               HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorBody(code, message,
                (String) request.getAttribute(RequestIdFilter.ATTRIBUTE)));
    }
}
