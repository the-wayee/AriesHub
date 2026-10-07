package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Interaction;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Progress;
import com.aries.backend.catalog.domain.model.PublicationReactionKind;

import java.util.List;
import java.util.Map;

/** 互动写入采用 MyBatis-Plus，个人列表与批量计数由只读联表投影提供。 */
public interface PublicationReaderRepository {
    boolean lockVisible(long publicationId);

    void reaction(long userId, long publicationId, PublicationReactionKind kind, boolean enabled);

    Map<String, Interaction> interactions(List<Long> ids, Long userId);

    Map<String, Progress> progress(List<Long> ids, long userId);

    void saveProgress(
            long userId, long publicationId, String version, String position, int percent);

    List<PublicationSummary> library(long userId, String kind, int page, int size);

    long libraryCount(long userId, String kind);
}
