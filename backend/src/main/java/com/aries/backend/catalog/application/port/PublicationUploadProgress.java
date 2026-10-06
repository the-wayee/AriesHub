package com.aries.backend.catalog.application.port;

import java.util.UUID;

/** 内容上传任务状态；owner 隔离、短期保存，不属于通用文件工具的业务。 */
public interface PublicationUploadProgress {
    record Status(String phase, long loaded, long total) {}

    void begin(long owner, UUID id, long total);

    void confirmed(long owner, UUID id, long loaded, long total);

    void completed(long owner, UUID id, long total);

    void failed(long owner, UUID id);

    Status status(long owner, UUID id);

    void cancel(long owner, UUID id);
}
