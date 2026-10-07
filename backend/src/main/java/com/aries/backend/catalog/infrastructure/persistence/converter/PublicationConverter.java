package com.aries.backend.catalog.infrastructure.persistence.converter;

import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationContentPO;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationPO;

/** 将存储表示转换成领域对象，隔离表字段和领域模型。 */
public final class PublicationConverter {
    private PublicationConverter() {}

    public static Publication toDomain(PublicationPO row, PublicationContentPO body) {
        return Publication.builder()
                .id(row.getId())
                .categoryId(row.getCategoryId())
                .coverFileId(row.getCoverFileId())
                .featured(Boolean.TRUE.equals(row.getFeatured()))
                .slug(row.getSlug())
                .title(row.getTitle())
                .summary(row.getSummary())
                .publicationType(Publication.PublicationType.valueOf(row.getPublicationType()))
                .accessType(Publication.AccessType.valueOf(row.getAccessType()))
                .creditPrice(row.getCreditPrice() == null ? 0 : row.getCreditPrice())
                .status(Publication.PublicationStatus.valueOf(row.getStatus()))
                .deliveryStatus(Publication.DeliveryStatus.valueOf(row.getDeliveryStatus()))
                .publishedAt(row.getPublishedAt())
                .content(
                        body == null
                                ? null
                                : new Publication.Content(
                                        body.getPreviewMarkdown(),
                                        body.getFullMarkdown(),
                                        body.getVersion()))
                .build();
    }

    public static PublicationPO toPO(Publication publication) {
        PublicationPO row = new PublicationPO();
        if (publication.getId() > 0) row.setId(publication.getId());
        row.setCategoryId(publication.getCategoryId());
        row.setSlug(publication.getSlug());
        row.setCoverFileId(publication.getCoverFileId());
        row.setFeatured(publication.isFeatured());
        row.setTitle(publication.getTitle());
        row.setSummary(publication.getSummary());
        row.setPublicationType(publication.getPublicationType().name());
        row.setAccessType(publication.getAccessType().name());
        row.setCreditPrice(publication.getCreditPrice());
        row.setStatus(publication.getStatus().name());
        row.setDeliveryStatus(publication.getDeliveryStatus().name());
        row.setPublishedAt(publication.getPublishedAt());
        return row;
    }

    public static PublicationContentPO toContentPO(
            long publicationId, Publication.Content content) {
        PublicationContentPO row = new PublicationContentPO();
        row.setPublicationId(publicationId);
        row.setPreviewMarkdown(content.previewMarkdown());
        row.setFullMarkdown(content.fullMarkdown());
        row.setVersion(content.version());
        return row;
    }
}
