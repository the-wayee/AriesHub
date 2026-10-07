package com.aries.backend.catalog.application.view;

import java.time.OffsetDateTime;
import java.util.List;

/** 公开查询的只读投影，不包含持久化对象或付费正文。 */
public final class CatalogViews {
    private CatalogViews() {}

    public record Category(
            String id, String slug, String name, String color, long publicationCount) {}

    // 此投影用于公开接口，禁止追加私有正文和对象存储 key。
    public record PublicationSummary(
            String id,
            String slug,
            String title,
            String summary,
            String categorySlug,
            String categoryName,
            String accessType,
            String publicationType,
            long creditPrice,
            OffsetDateTime publishedAt,
            String coverFileId,
            boolean featured) {}

    public record Preview(String previewMarkdown, String version, OffsetDateTime updatedAt) {}

    /** 目录只公开标题；headingIndex 仅指向获准阅读正文中的标题，不包含正文或资源地址。 */
    public record Chapter(String title, int level, boolean locked, Integer headingIndex) {}

    public record PublicationDetail(
            PublicationSummary publication, Preview preview, List<Chapter> chapters) {}

    public record Content(
            String publicationId, String markdown, String version, OffsetDateTime updatedAt) {}

    public record Page<T>(List<T> items, int page, int size, long total, long totalPages) {}
}
