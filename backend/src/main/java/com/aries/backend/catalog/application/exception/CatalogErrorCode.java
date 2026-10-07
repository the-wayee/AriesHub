package com.aries.backend.catalog.application.exception;

import com.aries.backend.shared.application.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 发布内容模块的稳定业务错误码。 */
@Getter
@RequiredArgsConstructor
public enum CatalogErrorCode implements ErrorCode {
    UPLOAD_TASK_UNAVAILABLE("上传任务已取消、过期或重复，请重新上传", Kind.CONFLICT),
    READING_VERSION_CHANGED("正文已更新，请重新加载后继续阅读", Kind.CONFLICT),
    INVALID_MEDIA("素材格式或大小不符合要求", Kind.BAD_REQUEST),
    MEDIA_NOT_FOUND("素材不存在或不能用于这篇内容", Kind.NOT_FOUND),
    PUBLICATION_NOT_FOUND("内容不存在或尚未公开", Kind.NOT_FOUND),
    CONTENT_LOCKED("该内容的完整正文尚未解锁", Kind.FORBIDDEN),
    ADMIN_PUBLICATION_NOT_FOUND("内容不存在", Kind.NOT_FOUND),
    CATEGORY_NOT_FOUND("所选分类不存在", Kind.BAD_REQUEST),
    PUBLICATION_SLUG_CONFLICT("内容地址已被使用", Kind.CONFLICT),
    PUBLICATION_CONTENT_REQUIRED("内容缺少完整正文，不能发布", Kind.CONFLICT);

    private final String message;
    private final Kind kind;
}
