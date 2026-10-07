package com.aries.backend.catalog.application.service;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.PUBLICATION_NOT_FOUND;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.READING_VERSION_CHANGED;

import com.aries.backend.catalog.application.port.CatalogReadPort;
import com.aries.backend.catalog.application.port.PublicationReaderIdentity;
import com.aries.backend.catalog.application.port.PublicationReaderRepository;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Interaction;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Progress;
import com.aries.backend.catalog.domain.model.PublicationReactionKind;
import com.aries.backend.shared.application.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public Interaction interaction(long id) {
        catalog.detail(id);
        return repository
                .interactions(List.of(id), identity.optionalUserId())
                .get(Long.toString(id));
    }

    public Progress progress(long id) {
        long user = identity.requireUserId();
        catalog.content(id);
        return repository.progress(List.of(id), user).get(Long.toString(id));
    }

    @Transactional
    public Progress saveProgress(long id, String version, String position, int percent) {
        long user = identity.requireUserId();
        if (!repository.lockVisible(id)) throw new BusinessException(PUBLICATION_NOT_FOUND);
        catalog.content(id);
        if (!reads.preview(id).version().equals(version))
            throw new BusinessException(READING_VERSION_CHANGED);
        repository.saveProgress(user, id, version, position, percent);
        return repository.progress(List.of(id), user).get(Long.toString(id));
    }
}
