package com.aries.backend.catalog.application.view;

import com.aries.backend.catalog.application.port.PublicationMediaPort.SignedUrl;
import com.aries.backend.catalog.application.view.CatalogViews.Category;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;

import java.time.OffsetDateTime;
import java.util.List;

/** 只返回发布摘要、互动状态与阅读位置，永远不携带私有正文。 */
public final class PublicationReaderViews {
    private PublicationReaderViews() {}

    public record Interaction(
            String publicationId, long likeCount, boolean liked, boolean bookmarked) {}

    public record Progress(
            String publicationId,
            String version,
            String position,
            int percent,
            OffsetDateTime updatedAt) {}

    /** 文章列表和首页的只读返回 DTO：组合摘要、封面和个人状态，不代表数据库实体或领域聚合。 */
    public record PublicationCardView(
            PublicationSummary publication,
            SignedUrl cover,
            Interaction interaction,
            Progress progress) {}

    public record Home(
            List<Category> categories,
            List<PublicationCardView> featured,
            List<PublicationCardView> latest,
            List<PublicationCardView> continueReading) {}
}
