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
            String publicationId,
            long likeCount,
            boolean liked,
            boolean bookmarked,
            long bookmarkCount,
            long shareCount,
            long viewCount) {}

    /** 动态仅展示已发布内容和互动者昵称，不包含正文或读者浏览记录。 */
    public record Activity(
            String id,
            String publicationId,
            String title,
            String userId,
            String actorName,
            String kind,
            OffsetDateTime createdAt) {}

    /** 稳定的站点分享链接，不包含私有素材地址。 */
    public record ShareLink(
            String publicationId, String url, String token, SignedUrl cover, String summary) {}

    /** 分享落地页仅提供公开试读和封面；全文通过专用授权端点读取。 */
    public record SharedPublication(
            CatalogViews.PublicationDetail detail,
            SignedUrl cover,
            String sharedBy,
            boolean canRead,
            String sharedAvatarUrl) {}

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
