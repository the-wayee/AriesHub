package com.aries.backend.catalog.application.exception;

import com.aries.backend.shared.application.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 发布内容模块的稳定业务错误码。 */
@Getter
@RequiredArgsConstructor
public enum CatalogErrorCode implements ErrorCode {
    PUBLICATION_NOT_FOUND("内容不存在或尚未公开", Kind.NOT_FOUND),
    CONTENT_LOCKED("该内容的完整正文尚未解锁", Kind.FORBIDDEN),
    ADMIN_PUBLICATION_NOT_FOUND("内容不存在", Kind.NOT_FOUND),
    CATEGORY_NOT_FOUND("所选分类不存在", Kind.BAD_REQUEST),
    PUBLICATION_SLUG_CONFLICT("内容地址已被使用", Kind.CONFLICT),
    PUBLICATION_CONTENT_REQUIRED("内容缺少完整正文，不能发布", Kind.CONFLICT);

    private final String message;
    private final Kind kind;
}
