package com.aries.backend.catalog.infrastructure.persistence.converter;

import com.aries.backend.catalog.domain.model.CaseStudy;
import com.aries.backend.catalog.infrastructure.persistence.po.CasePO;
import com.aries.backend.catalog.infrastructure.persistence.po.CaseContentPO;

/** 将存储表示转换成领域对象，隔离表字段和领域模型。 */
public final class CaseConverter {
    private CaseConverter() {}

    public static CaseStudy toDomain(CasePO row) {
        return toDomain(row, null);
    }

    public static CaseStudy toDomain(CasePO row, CaseContentPO body) {
        return CaseStudy.builder()
                .id(row.getId()).categoryId(row.getCategoryId())
                .slug(row.getSlug()).title(row.getTitle()).summary(row.getSummary())
                .accessType(CaseStudy.AccessType.valueOf(row.getAccessType()))
                .priceMinor(row.getPriceMinor()).currency(row.getCurrency())
                .status(CaseStudy.PublicationStatus.valueOf(row.getStatus()))
                .deliveryStatus(CaseStudy.DeliveryStatus.valueOf(row.getDeliveryStatus()))
                .demo(Boolean.TRUE.equals(row.getIsDemo())).publishedAt(row.getPublishedAt())
                .content(body == null ? null : new CaseStudy.Content(body.getPreviewMarkdown(),
                        body.getFullMarkdown(), body.getRequirements(), body.getDeliverables(),
                        body.getVersion()))
                .build();
    }

    public static CasePO toPO(CaseStudy study) {
        CasePO row = new CasePO();
        if (study.getId() > 0) row.setId(study.getId());
        row.setCategoryId(study.getCategoryId());
        row.setSlug(study.getSlug());
        row.setTitle(study.getTitle());
        row.setSummary(study.getSummary());
        row.setAccessType(study.getAccessType().name());
        row.setPriceMinor(study.getPriceMinor());
        row.setCurrency(study.getCurrency());
        row.setStatus(study.getStatus().name());
        row.setDeliveryStatus(study.getDeliveryStatus().name());
        row.setIsDemo(study.isDemo());
        row.setPublishedAt(study.getPublishedAt());
        return row;
    }

    public static CaseContentPO toContentPO(long caseId, CaseStudy.Content content) {
        CaseContentPO row = new CaseContentPO();
        row.setCaseId(caseId);
        row.setPreviewMarkdown(content.previewMarkdown());
        row.setFullMarkdown(content.fullMarkdown());
        row.setRequirements(content.requirements());
        row.setDeliverables(content.deliverables());
        row.setVersion(content.version());
        return row;
    }
}
