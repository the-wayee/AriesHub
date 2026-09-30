package com.aries.backend.storage.domain.model;

import java.util.UUID;

/** 业务引用文件 ID，不保存临时签名 URL。 */
public record StoredFile(UUID id, long ownerId, Purpose purpose, String objectKey,
                         String filename, String contentType, long size) {
    public enum Purpose { AVATAR, ATTACHMENT }
}
