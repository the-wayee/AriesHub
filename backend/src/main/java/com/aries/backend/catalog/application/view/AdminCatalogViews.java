package com.aries.backend.catalog.application.view;

import java.time.OffsetDateTime;

/** 内容后台使用的完整编辑视图，仅管理员接口可以返回。 */
public final class AdminCatalogViews {
    private AdminCatalogViews() {}

    public record AdminPublicationSummary(
            String id,
            String slug,
            String title,
            String categoryName,
            String publicationType,
            String accessType,
            long creditPrice,
            String status,
            String deliveryStatus,
            OffsetDateTime publishedAt,
            OffsetDateTime updatedAt,
            String coverFileId,
            boolean featured) {}

    public record AdminPublicationDetail(
            String id,
            String categoryId,
            String slug,
            String title,
            String summary,
            String publicationType,
            String accessType,
            long creditPrice,
            String status,
            String deliveryStatus,
            OffsetDateTime publishedAt,
            String previewMarkdown,
            String fullMarkdown,
            String requirements,
            String deliverables,
            String version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String coverFileId,
            boolean featured) {}
}
