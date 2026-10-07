package com.aries.backend.activity.infrastructure.persistence;

import com.aries.backend.activity.domain.model.CommunityEvent;
import com.aries.backend.activity.domain.model.CommunityEventKind;
import com.aries.backend.activity.domain.model.CommunitySubjectType;
import com.aries.backend.activity.domain.repository.CommunityEventRepository;
import com.aries.backend.activity.infrastructure.persistence.mapper.CommunityEventMapper;
import com.aries.backend.activity.infrastructure.persistence.po.CommunityEventPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

/** 枚举只在存储边界转换编码；唯一事件键交给数据库原子去重，避免并发重复动态。 */
@Repository
@RequiredArgsConstructor
public class MybatisCommunityEventRepository implements CommunityEventRepository {
    private final CommunityEventMapper mapper;

    @Override
    public void append(CommunityEvent event) {
        CommunityEventPO po = new CommunityEventPO();
        po.setEventKey(event.eventKey());
        po.setActorId(event.actorId());
        po.setKind(event.kind().name());
        po.setSubjectType(event.subjectType().name());
        po.setSubjectId(event.subjectId());
        po.setCreatedAt(event.createdAt());
        mapper.appendOnce(po);
    }

    /** 按记录 ID 稳定分页；新增动态不会挤动已加载的页，筛选在分页之前执行。 */
    @Override
    public List<CommunityEvent> recent(int size, Long before, List<CommunityEventKind> kinds) {
        return mapper
                .selectPage(
                        new Page<CommunityEventPO>(1, size, false),
                        Wrappers.<CommunityEventPO>lambdaQuery()
                                .lt(before != null, CommunityEventPO::getId, before)
                                .in(
                                        !kinds.isEmpty(),
                                        CommunityEventPO::getKind,
                                        kinds.stream().map(Enum::name).toList())
                                .orderByDesc(CommunityEventPO::getId))
                .getRecords()
                .stream()
                .map(
                        po ->
                                new CommunityEvent(
                                        po.getId(),
                                        po.getEventKey(),
                                        po.getActorId(),
                                        CommunityEventKind.valueOf(po.getKind()),
                                        CommunitySubjectType.valueOf(po.getSubjectType()),
                                        po.getSubjectId(),
                                        po.getCreatedAt()))
                .toList();
    }
}
