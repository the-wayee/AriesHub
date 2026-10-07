package com.aries.backend.catalog.application.service;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.PUBLICATION_NOT_FOUND;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.READING_VERSION_CHANGED;

import com.aries.backend.catalog.application.event.PublicationActivityOccurred;
import com.aries.backend.catalog.application.port.CatalogReadPort;
import com.aries.backend.catalog.application.port.PublicationReaderIdentity;
import com.aries.backend.catalog.application.port.PublicationReaderRepository;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Activity;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Interaction;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Progress;
import com.aries.backend.catalog.domain.model.PublicationEventKind;
import com.aries.backend.catalog.domain.model.PublicationReactionKind;
import com.aries.backend.shared.application.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 当前用户关系与阅读用例；账号身份、发布状态和免费正文授权均由后端验证。 */
@Service
@RequiredArgsConstructor
public class PublicationReaderService {
    private final PublicationReaderRepository repository;
    private final PublicationReaderIdentity identity;
    private final CatalogQueryService catalog;
    private final CatalogReadPort reads;
    private final ApplicationEventPublisher events;

    /** 登录后设置点赞或收藏；文章行锁与关系去重保证并发下架及重复提交时一致。 */
    @Transactional
    public Interaction reaction(long id, PublicationReactionKind kind, boolean enabled) {
        long user = identity.requireUserId();
        if (!repository.lockVisible(id)) throw new BusinessException(PUBLICATION_NOT_FOUND);
        catalog.detail(id);
        // 仅真正新增关系才发布动态，重复 PUT 和取消操作不生成新动态。
        if (repository.reaction(user, id, kind, enabled))
            events.publishEvent(
                    new PublicationActivityOccurred(
                            "reaction:" + UUID.randomUUID(),
                            user,
                            id,
                            PublicationActivityOccurred.Kind.valueOf(kind.name())));
        return repository.interactions(List.of(id), user).get(Long.toString(id));
    }

    private static final long VIEW_WINDOW_SECONDS = 30 * 60;

    /** 阅读按账号或匿名浏览器每半小时去重；分享确认只由分享授权用例处理。 */
    @Transactional
    public Interaction view(long id, String token) {
        Long user = identity.optionalUserId();
        if (!repository.lockVisible(id)) throw new BusinessException(PUBLICATION_NOT_FOUND);
        String actor = user == null ? "visitor:" + token : "user:" + user;
        String key = actor + ":" + Instant.now().getEpochSecond() / VIEW_WINDOW_SECONDS;
        repository.event(user, id, PublicationEventKind.VIEW, key);
        return repository.interactions(List.of(id), user).get(Long.toString(id));
    }

    public List<Activity> activity(int size) {
        identity.requireUserId();
        List<Activity> rows = repository.activity(size);
        Set<Long> ids =
                rows.stream().map(row -> Long.parseLong(row.userId())).collect(Collectors.toSet());
        Map<Long, String> names = identity.displayNames(ids);
        return rows.stream()
                .map(
                        row ->
                                new Activity(
                                        row.id(),
                                        row.publicationId(),
                                        row.title(),
                                        row.userId(),
                                        names.getOrDefault(Long.parseLong(row.userId()), "社区成员"),
                                        row.kind(),
                                        row.createdAt()))
                .toList();
    }

    /** 返回公开文章的互动计数，并在已登录时附带当前用户的点赞、收藏状态。 */
    public Interaction interaction(long id) {
        catalog.detail(id);
        return repository
                .interactions(List.of(id), identity.optionalUserId())
                .get(Long.toString(id));
    }

    /** 只有获准阅读全文的账号能查询自己的阅读位置，不能据此探测付费正文。 */
    public Progress progress(long id) {
        long user = identity.requireUserId();
        catalog.memberContent(id);
        return repository.progress(List.of(id), user).get(Long.toString(id));
    }

    /** 写入前校验阅读权限和正文版本；正文变动拒绝旧位置，元数据变动不会改变版本。 */
    @Transactional
    public Progress saveProgress(long id, String version, String position, int percent) {
        long user = identity.requireUserId();
        if (!repository.lockVisible(id)) throw new BusinessException(PUBLICATION_NOT_FOUND);
        catalog.memberContent(id);
        if (!reads.preview(id).version().equals(version))
            throw new BusinessException(READING_VERSION_CHANGED);
        repository.saveProgress(user, id, version, position, percent);
        return repository.progress(List.of(id), user).get(Long.toString(id));
    }
}
