package com.aries.backend.catalog.application.port;

import java.util.Optional;

/** 分享链接仅绑定一个内容与一个分享人，重复生成返回同一份授权。 */
public interface PublicationShareRepository {
    record Share(long id, long publicationId, long userId, String token) {}

    Share getOrCreate(long userId, long publicationId);

    Optional<Share> find(String token);
}
