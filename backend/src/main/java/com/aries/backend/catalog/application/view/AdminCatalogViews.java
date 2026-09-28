package com.aries.backend.catalog.application.view;

import java.time.OffsetDateTime;

/** 内容后台使用的完整编辑视图，仅管理员接口可以返回。 */
public final class AdminCatalogViews {
    private AdminCatalogViews() {}

    public record CategoryOption(String id, String slug, String name) {}

    public record CaseSummary(
            String id,
            String slug,
            String title,
            String categoryName,
            String accessType,
            long priceMinor,
            String status,
            String deliveryStatus,
            OffsetDateTime publishedAt,
            OffsetDateTime updatedAt
    ) {}

    public record CaseDetail(
            String id,
            String categoryId,
            String slug,
            String title,
            String summary,
            String accessType,
            long priceMinor,
            String currency,
            String status,
            String deliveryStatus,
            boolean isDemo,
            OffsetDateTime publishedAt,
            String previewMarkdown,
            String fullMarkdown,
            String requirements,
            String deliverables,
            String version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}
}
