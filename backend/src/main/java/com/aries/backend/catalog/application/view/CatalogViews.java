package com.aries.backend.catalog.application.view;

import java.time.OffsetDateTime;
import java.util.List;

/** 公开查询的只读投影，不包含持久化对象或付费正文。 */
public final class CatalogViews {
    private CatalogViews() {}

    public record Category(String id, String slug, String name, long caseCount) {}
    // 此投影用于公开接口，禁止追加私有正文和对象存储 key。
    public record CaseSummary(String id, String slug, String title, String summary,
                              String categorySlug, String categoryName, String accessType,
                              long priceMinor, String currency, boolean isDemo, OffsetDateTime publishedAt) {}
    public record Preview(String previewMarkdown, String requirements, String deliverables,
                          String version, OffsetDateTime updatedAt) {}
    public record CaseDetail(CaseSummary caseInfo, Preview preview) {}
    public record Content(String caseId, String markdown, String version, OffsetDateTime updatedAt) {}
    public record Page<T>(List<T> items, int page, int size, long total, long totalPages) {}
}
