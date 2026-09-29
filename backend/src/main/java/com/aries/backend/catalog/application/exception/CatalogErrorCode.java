package com.aries.backend.catalog.application.exception;

import com.aries.backend.shared.application.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 案例模块的稳定业务错误码。 */
@Getter
@RequiredArgsConstructor
public enum CatalogErrorCode implements ErrorCode {
    CASE_NOT_FOUND("案例不存在或尚未公开", Kind.NOT_FOUND),
    CONTENT_LOCKED("该案例的完整内容暂未开放访问", Kind.FORBIDDEN),
    ADMIN_CASE_NOT_FOUND("案例不存在", Kind.NOT_FOUND),
    CATEGORY_NOT_FOUND("所选分类不存在", Kind.BAD_REQUEST),
    CASE_SLUG_CONFLICT("案例地址已被使用", Kind.CONFLICT),
    CASE_CONTENT_REQUIRED("案例缺少完整正文，不能发布", Kind.CONFLICT);

    private final String message;
    private final Kind kind;
}
