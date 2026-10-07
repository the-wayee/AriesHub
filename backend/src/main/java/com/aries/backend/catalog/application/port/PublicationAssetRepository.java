package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.port.PublicationMediaPort.Asset;

import java.util.List;
import java.util.Optional;

/** 内容素材及文章绑定端口；文件元数据不暴露存储 key 或永久公开地址。 */
public interface PublicationAssetRepository {
    void save(Asset asset);

    Optional<Asset> find(String id);

    boolean bound(long publicationId, String id);

    boolean publiclyVisible(long publicationId, String id);

    /** 由文章编辑事务调用；替换引用但不删除对象，避免破坏其他文章绑定。 */
    void replaceBindings(
            long publicationId,
            List<String> all,
            List<String> publicIds,
            List<String> attachmentIds);

    List<Asset> attachments(long publicationId);

    List<String> resourceIds(long publicationId);
}
