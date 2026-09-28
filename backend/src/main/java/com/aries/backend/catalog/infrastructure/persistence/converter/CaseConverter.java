package com.aries.backend.catalog.infrastructure.persistence.converter;

import com.aries.backend.catalog.domain.model.CaseStudy;
import com.aries.backend.catalog.infrastructure.persistence.po.CasePO;

/** 将存储表示转换成领域对象，隔离表字段和领域模型。 */
public final class CaseConverter {
    private CaseConverter() {}

    public static CaseStudy toDomain(CasePO row) {
        return CaseStudy.builder()
                .id(row.getId()).categoryId(row.getCategoryId())
                .slug(row.getSlug()).title(row.getTitle()).summary(row.getSummary())
                .accessType(CaseStudy.AccessType.valueOf(row.getAccessType()))
                .priceMinor(row.getPriceMinor()).currency(row.getCurrency())
                .status(CaseStudy.PublicationStatus.valueOf(row.getStatus()))
                .deliveryStatus(CaseStudy.DeliveryStatus.valueOf(row.getDeliveryStatus()))
                .demo(Boolean.TRUE.equals(row.getIsDemo())).publishedAt(row.getPublishedAt()).build();
    }
}
