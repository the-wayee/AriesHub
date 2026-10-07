package com.aries.backend.catalog.application.service;

import com.aries.backend.catalog.application.port.PublicationCoverPort;
import com.aries.backend.catalog.application.port.PublicationMediaPort.SignedUrl;
import com.aries.backend.catalog.application.port.PublicationReaderIdentity;
import com.aries.backend.catalog.application.port.PublicationReaderRepository;
import com.aries.backend.catalog.application.query.PublicationSearchQuery;
import com.aries.backend.catalog.application.view.CatalogViews.Page;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Home;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Interaction;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Progress;
import com.aries.backend.catalog.application.view.PublicationReaderViews.PublicationCardView;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 批量补齐卡片封面与个人状态；避免每张卡片单独查关系或取素材元数据。 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicationCardService {
    private final CatalogQueryService catalog;
    private final PublicationReaderRepository readers;
    private final PublicationReaderIdentity identity;
    private final PublicationCoverPort covers;

    public Page<PublicationCardView> list(PublicationSearchQuery query) {
        Page<PublicationSummary> page = catalog.list(query);
        return new Page<>(
                cards(page.items(), identity.optionalUserId()),
                page.page(),
                page.size(),
                page.total(),
                page.totalPages());
    }

    public Page<PublicationCardView> library(String kind, int page, int size) {
        long user = identity.requireUserId();
        long total = readers.libraryCount(user, kind);
        return new Page<>(
                cards(readers.library(user, kind, page, size), user),
                page,
                size,
                total,
                (total + size - 1) / size);
    }

    public Home home() {
        long user = identity.requireUserId();
        return new Home(
                catalog.categories(),
                cards(
                        catalog.list(
                                        new PublicationSearchQuery(
                                                1, 4, "", "", "", "", "FEATURED", true))
                                .items(),
                        user),
                cards(
                        catalog.list(
                                        new PublicationSearchQuery(
                                                1, 6, "", "", "", "", "LATEST", null))
                                .items(),
                        user),
                cards(readers.library(user, "HISTORY", 1, 3), user));
    }

    private List<PublicationCardView> cards(List<PublicationSummary> items, Long user) {
        List<Long> ids = items.stream().map(p -> Long.parseLong(p.id())).toList();
        Map<String, Interaction> state = readers.interactions(ids, user);
        Map<String, Progress> progress =
                user == null ? Map.<String, Progress>of() : readers.progress(ids, user);
        Map<String, SignedUrl> urls =
                covers.sign(
                        items.stream()
                                .map(PublicationSummary::coverFileId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList());
        return items.stream()
                .map(
                        p ->
                                new PublicationCardView(
                                        p,
                                        urls.get(p.coverFileId()),
                                        state.get(p.id()),
                                        progress.get(p.id())))
                .toList();
    }
}
