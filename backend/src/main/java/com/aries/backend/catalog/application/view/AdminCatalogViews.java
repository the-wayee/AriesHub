package com.aries.backend.catalog.application.view;

import com.aries.backend.catalog.application.port.PublicationMediaPort.SignedUrl;

import java.time.OffsetDateTime;

/** 内容后台使用的完整编辑视图，仅管理员接口可以返回。 */
public final class AdminCatalogViews {
    private AdminCatalogViews() {}

    public record AdminPublicationSummary(
            String id,
            String slug,
            String title,
            String categoryName,
            String categoryColor,
            String publicationType,
            String accessType,
            long creditPrice,
            String status,
            String deliveryStatus,
            OffsetDateTime publishedAt,
            OffsetDateTime updatedAt,
            String coverFileId,
            boolean featured) {}

    /** 列表响应在查询投影上补齐临时封面地址，不将签名写入数据库。 */
    public record AdminPublicationListItem(
            String id,
            String slug,
            String title,
            String categoryName,
            String categoryColor,
            String publicationType,
            String accessType,
            long creditPrice,
            String status,
            String deliveryStatus,
            OffsetDateTime publishedAt,
            OffsetDateTime updatedAt,
            String coverFileId,
            boolean featured,
            SignedUrl cover) {
        public AdminPublicationListItem(AdminPublicationSummary item, SignedUrl cover) {
            this(
                    item.id(),
                    item.slug(),
                    item.title(),
                    item.categoryName(),
                    item.categoryColor(),
                    item.publicationType(),
                    item.accessType(),
                    item.creditPrice(),
                    item.status(),
                    item.deliveryStatus(),
                    item.publishedAt(),
                    item.updatedAt(),
                    item.coverFileId(),
                    item.featured(),
                    cover);
        }
    }

    public record AdminPublicationDetailRow(
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

    /** 编辑响应补齐私有封面签名，URL 不参与持久化。 */
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
            boolean featured,
            SignedUrl cover) {
        public AdminPublicationDetail(AdminPublicationDetailRow row, SignedUrl cover) {
            this(
                    row.id(),
                    row.categoryId(),
                    row.slug(),
                    row.title(),
                    row.summary(),
                    row.publicationType(),
                    row.accessType(),
                    row.creditPrice(),
                    row.status(),
                    row.deliveryStatus(),
                    row.publishedAt(),
                    row.previewMarkdown(),
                    row.fullMarkdown(),
                    row.requirements(),
                    row.deliverables(),
                    row.version(),
                    row.createdAt(),
                    row.updatedAt(),
                    row.coverFileId(),
                    row.featured(),
                    cover);
        }
    }
}
