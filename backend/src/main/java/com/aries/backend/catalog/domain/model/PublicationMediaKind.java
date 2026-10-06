package com.aries.backend.catalog.domain.model;

/** 内容素材用途，与通用存储的技术用途分离，避免任意字符串散落在授权逻辑中。 */
public enum PublicationMediaKind {
    COVER,
    IMAGE,
    VIDEO,
    ATTACHMENT;

    public boolean isImage() {
        return this == COVER || this == IMAGE;
    }
}
