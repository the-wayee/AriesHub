package com.aries.backend.storage.application.exception;

import com.aries.backend.shared.application.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StorageErrorCode implements ErrorCode {
    INVALID_FILE("文件类型、内容或大小不符合要求", Kind.BAD_REQUEST),
    FILE_NOT_FOUND("文件不存在或无权访问", Kind.NOT_FOUND),
    STORAGE_UNAVAILABLE("文件存储暂时不可用，请稍后重试", Kind.SERVICE_UNAVAILABLE),
    UPLOAD_RATE_LIMITED("上传过于频繁，请稍后重试", Kind.TOO_MANY_REQUESTS);
    private final String message;
    private final Kind kind;
}
