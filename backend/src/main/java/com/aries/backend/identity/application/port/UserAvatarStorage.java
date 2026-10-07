package com.aries.backend.identity.application.port;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** 头像文件归属与展示签名端口，由 composition 桥接存储模块。 */
public interface UserAvatarStorage {
    String AVATAR_PURPOSE = "AVATAR";

    File metadata(UUID fileId);

    File upload(long userId, String filename, String contentType, long size, InputStream content);

    void checkUploadRate(long userId);

    Image inlineUrl(UUID fileId);

    /** 批量签发已绑定的公开头像，必须检查文件用途与账号归属；缺失文件不返回。 */
    Map<Long, Image> publicAvatars(Map<Long, UUID> avatarIds);

    record File(
            UUID id,
            long ownerId,
            String purpose,
            String filename,
            String contentType,
            long size) {}

    record Image(String url, Instant expiresAt) {}
}
