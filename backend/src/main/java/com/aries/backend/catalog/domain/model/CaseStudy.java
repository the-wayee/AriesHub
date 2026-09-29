package com.aries.backend.catalog.domain.model;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;

/**
 * 案例聚合根：集中表达编辑、发布、下架和公开阅读规则。
 * 不依赖 Spring、MyBatis-Plus 或 HTTP；正文由单独的受控读取入口提供。
 * 不开放通用 setter，避免后续业务绕过发布、定价等行为直接修改聚合状态。
 */
@Value
@Builder(toBuilder = true)
public class CaseStudy {
    long id;
    long categoryId;
    String slug;
    String title;
    String summary;
    AccessType accessType;
    long priceMinor;
    String currency;
    PublicationStatus status;
    DeliveryStatus deliveryStatus;
    boolean demo;
    OffsetDateTime publishedAt;
    Content content;

    public enum AccessType { FREE, PAID }
    public enum PublicationStatus { DRAFT, PUBLISHED, ARCHIVED }
    public enum DeliveryStatus { AVAILABLE, SUSPENDED }

    public record Content(String previewMarkdown, String fullMarkdown, String requirements,
                          String deliverables, String version) {}

    public record Draft(long categoryId, String slug, String title, String summary,
                        AccessType accessType, long priceMinor, Content content) {}

    public static class MissingContent extends RuntimeException {}

    public static CaseStudy create(Draft draft) {
        validate(draft);
        return CaseStudy.builder()
                .categoryId(draft.categoryId()).slug(draft.slug()).title(draft.title())
                .summary(draft.summary()).accessType(draft.accessType())
                .priceMinor(draft.priceMinor()).content(draft.content())
                .currency("CNY").status(PublicationStatus.DRAFT)
                .deliveryStatus(DeliveryStatus.AVAILABLE).demo(false).build();
    }

    public CaseStudy edit(Draft draft) {
        validate(draft);
        if (status == PublicationStatus.PUBLISHED && !hasFullContent(draft.content())) {
            throw new MissingContent();
        }
        return toBuilder()
                .categoryId(draft.categoryId()).slug(draft.slug()).title(draft.title())
                .summary(draft.summary()).accessType(draft.accessType())
                .priceMinor(draft.priceMinor()).content(draft.content()).build();
    }

    /** 只有具备完整正文的案例可以发布；时间由应用用例注入。 */
    public CaseStudy publish(OffsetDateTime now) {
        if (!hasFullContent(content)) {
            throw new MissingContent();
        }
        return toBuilder().status(PublicationStatus.PUBLISHED)
                .deliveryStatus(DeliveryStatus.AVAILABLE).publishedAt(now).build();
    }

    public CaseStudy archive() {
        return toBuilder().status(PublicationStatus.ARCHIVED).build();
    }

    private static boolean hasFullContent(Content content) {
        return content != null && content.fullMarkdown() != null && !content.fullMarkdown().isBlank();
    }

    private static void validate(Draft draft) {
        if (draft.categoryId() <= 0 || draft.accessType() == null || draft.content() == null ||
                draft.slug() == null || draft.slug().isBlank() ||
                draft.title() == null || draft.title().isBlank() ||
                draft.summary() == null || draft.summary().isBlank() ||
                draft.priceMinor() < 0 ||
                (draft.accessType() == AccessType.FREE && draft.priceMinor() != 0) ||
                (draft.accessType() == AccessType.PAID && draft.priceMinor() == 0)) {
            throw new IllegalArgumentException("案例草稿无效");
        }
    }

    /** 草稿、下架或暂停交付时，公开查询一律视为不存在。 */
    public boolean isPubliclyVisible() {
        return status == PublicationStatus.PUBLISHED && deliveryStatus == DeliveryStatus.AVAILABLE;
    }

    /** M1 只允许免费阅读；后续付费权益由独立的权益领域判断。 */
    public boolean allowsPublicReading() {
        return isPubliclyVisible() && accessType == AccessType.FREE;
    }
}
