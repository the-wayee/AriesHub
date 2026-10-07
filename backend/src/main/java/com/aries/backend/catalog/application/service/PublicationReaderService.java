package com.aries.backend.catalog.application.service;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.PUBLICATION_NOT_FOUND;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.READING_VERSION_CHANGED;

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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 当前用户关系与阅读用例；账号身份、发布状态和免费正文授权均由后端验证。 */
@Service
@RequiredArgsConstructor
public class PublicationReaderService {
    private final PublicationReaderRepository repository;
    private final PublicationReaderIdentity identity;
    private final CatalogQueryService catalog;
    private final CatalogReadPort reads;

    @Transactional
    public Interaction reaction(long id, PublicationReactionKind kind, boolean enabled) {
        long user = identity.requireUserId();
        if (!repository.lockVisible(id)) throw new BusinessException(PUBLICATION_NOT_FOUND);
        catalog.detail(id);
        repository.reaction(user, id, kind, enabled);
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

    public Interaction interaction(long id) {
        catalog.detail(id);
        return repository
                .interactions(List.of(id), identity.optionalUserId())
                .get(Long.toString(id));
    }

    public Progress progress(long id) {
        long user = identity.requireUserId();
        catalog.memberContent(id);
        return repository.progress(List.of(id), user).get(Long.toString(id));
    }

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
