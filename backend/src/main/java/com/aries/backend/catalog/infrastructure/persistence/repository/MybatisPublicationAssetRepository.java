package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.application.port.PublicationAssetRepository;
import com.aries.backend.catalog.application.port.PublicationMediaPort;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationAssetMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationMediaBindingMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationAssetPO;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationMediaBindingPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 素材元数据及绑定使用 BaseMapper；完整替换在内容编辑事务内执行。 */
@Repository
@RequiredArgsConstructor
public class MybatisPublicationAssetRepository implements PublicationAssetRepository {
    private final PublicationAssetMapper assets;
    private final PublicationMediaBindingMapper bindings;

    public void save(PublicationMediaPort.Asset asset) {
        PublicationAssetPO po = new PublicationAssetPO();
        po.setId(asset.id());
        po.setOwnerId(asset.ownerId());
        po.setKind(asset.kind());
        po.setFilename(asset.filename());
        po.setContentType(asset.contentType());
        po.setSize(asset.size());
        assets.insert(po);
    }

    public Optional<PublicationMediaPort.Asset> find(String id) {
        return Optional.ofNullable(assets.selectById(id))
                .map(
                        p ->
                                new PublicationMediaPort.Asset(
                                        p.getId(),
                                        p.getOwnerId(),
                                        p.getKind(),
                                        p.getFilename(),
                                        p.getContentType(),
                                        p.getSize()));
    }

    public boolean bound(long publicationId, String id) {
        return bindings.selectCount(
                        Wrappers.<PublicationMediaBindingPO>lambdaQuery()
                                .eq(PublicationMediaBindingPO::getPublicationId, publicationId)
                                .eq(PublicationMediaBindingPO::getFileId, id))
                > 0;
    }

    public boolean publiclyVisible(long publicationId, String id) {
        return bindings.selectCount(
                        Wrappers.<PublicationMediaBindingPO>lambdaQuery()
                                .eq(PublicationMediaBindingPO::getPublicationId, publicationId)
                                .eq(PublicationMediaBindingPO::getFileId, id)
                                .eq(PublicationMediaBindingPO::getPubliclyVisible, true))
                > 0;
    }

    /** 在外层内容事务中先删后插；数据库联合主键保证同一素材不会重复绑定。 */
    public void replaceBindings(long publicationId, List<String> all, List<String> publicIds) {
        bindings.delete(
                Wrappers.<PublicationMediaBindingPO>lambdaQuery()
                        .eq(PublicationMediaBindingPO::getPublicationId, publicationId));
        for (String id : all) {
            PublicationMediaBindingPO po = new PublicationMediaBindingPO();
            po.setPublicationId(publicationId);
            po.setFileId(id);
            po.setPubliclyVisible(publicIds.contains(id));
            bindings.insert(po);
        }
    }
}
