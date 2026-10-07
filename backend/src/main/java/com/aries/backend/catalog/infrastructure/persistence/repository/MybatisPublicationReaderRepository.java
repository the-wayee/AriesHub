package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.application.port.PublicationReaderRepository;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Interaction;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Progress;
import com.aries.backend.catalog.domain.model.PublicationReactionKind;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationReactionMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationReaderReadMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationReadingMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationPO;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationReactionPO;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationReadingPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 用发布内容行锁串行化关系变更，避免查询后插入的并发重复；不使用拼接的用户 SQL。 */
@Repository
@RequiredArgsConstructor
public class MybatisPublicationReaderRepository implements PublicationReaderRepository {
    private final PublicationMapper publications;
    private final PublicationReactionMapper reactions;
    private final PublicationReadingMapper reading;
    private final PublicationReaderReadMapper reads;

    public boolean lockVisible(long id) {
        return publications.selectOne(
                        Wrappers.<PublicationPO>lambdaQuery()
                                .eq(PublicationPO::getId, id)
                                .eq(PublicationPO::getStatus, "PUBLISHED")
                                .eq(PublicationPO::getDeliveryStatus, "AVAILABLE")
                                .last("FOR UPDATE"))
                != null;
    }

    public void reaction(long user, long id, PublicationReactionKind kind, boolean enabled) {
        LambdaQueryWrapper<PublicationReactionPO> query =
                Wrappers.<PublicationReactionPO>lambdaQuery()
                        .eq(PublicationReactionPO::getUserId, user)
                        .eq(PublicationReactionPO::getPublicationId, id)
                        .eq(PublicationReactionPO::getKind, kind.name());
        if (!enabled) {
            reactions.delete(query);
            return;
        }
        if (reactions.selectCount(query) > 0) return;
        PublicationReactionPO po = new PublicationReactionPO();
        po.setUserId(user);
        po.setPublicationId(id);
        po.setKind(kind.name());
        po.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        reactions.insert(po);
    }

    public Map<String, Interaction> interactions(List<Long> ids, Long user) {
        if (ids.isEmpty()) return Map.of();
        return reads.interactions(ids, user).stream()
                .collect(Collectors.toMap(Interaction::publicationId, x -> x));
    }

    public Map<String, Progress> progress(List<Long> ids, long user) {
        if (ids.isEmpty()) return Map.of();
        return reading
                .selectList(
                        Wrappers.<PublicationReadingPO>lambdaQuery()
                                .eq(PublicationReadingPO::getUserId, user)
                                .in(PublicationReadingPO::getPublicationId, ids))
                .stream()
                .map(
                        p ->
                                new Progress(
                                        p.getPublicationId().toString(),
                                        p.getVersion(),
                                        p.getPosition(),
                                        p.getPercent(),
                                        p.getUpdatedAt()))
                .collect(Collectors.toMap(Progress::publicationId, x -> x));
    }

    public void saveProgress(long user, long id, String version, String position, int percent) {
        PublicationReadingPO po =
                reading.selectOne(
                        Wrappers.<PublicationReadingPO>lambdaQuery()
                                .eq(PublicationReadingPO::getUserId, user)
                                .eq(PublicationReadingPO::getPublicationId, id));
        boolean create = po == null;
        if (create) {
            po = new PublicationReadingPO();
            po.setUserId(user);
            po.setPublicationId(id);
        }
        po.setVersion(version);
        po.setPosition(position);
        po.setPercent(percent);
        po.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (create) reading.insert(po);
        else reading.updateById(po);
    }

    public List<PublicationSummary> library(long user, String kind, int page, int size) {
        return reads.library(user, kind, (page - 1) * size, size);
    }

    public long libraryCount(long user, String kind) {
        return reads.libraryCount(user, kind);
    }
}
