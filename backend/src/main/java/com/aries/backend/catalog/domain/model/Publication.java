package com.aries.backend.catalog.domain.model;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;

/**
 * 发布内容聚合根：集中表达编辑、发布、下架和公开阅读规则。
 * 不依赖 Spring、MyBatis-Plus 或 HTTP；正文由单独的受控读取入口提供。
 * 不开放通用 setter，避免后续业务绕过发布、定价等行为直接修改聚合状态。
 */
@Value
@Builder(toBuilder = true)
public class Publication {
    long id;
    long categoryId;
    String slug;
    String title;
    String summary;
    PublicationType publicationType;
    AccessType accessType;
    long creditPrice;
    PublicationStatus status;
    DeliveryStatus deliveryStatus;
    OffsetDateTime publishedAt;
    Content content;

    public enum PublicationType { CASE_STUDY, ARTICLE, COURSE }
    public enum AccessType { FREE, CREDIT }
    public enum PublicationStatus { DRAFT, PUBLISHED, ARCHIVED }
    public enum DeliveryStatus { AVAILABLE, SUSPENDED }

    public record Content(String previewMarkdown, String fullMarkdown, String requirements,
                          String deliverables, String version) {}

    public record Draft(long categoryId, String slug, String title, String summary,
                        PublicationType publicationType, AccessType accessType,
                        long creditPrice, Content content) {}

    public static class MissingContent extends RuntimeException {}

    public static Publication create(Draft draft) {
        validate(draft);
        return Publication.builder()
                .categoryId(draft.categoryId()).slug(draft.slug()).title(draft.title())
                .summary(draft.summary()).publicationType(draft.publicationType())
                .accessType(draft.accessType()).creditPrice(draft.creditPrice()).content(draft.content())
                .status(PublicationStatus.DRAFT)
                .deliveryStatus(DeliveryStatus.AVAILABLE).build();
    }

    public Publication edit(Draft draft) {
        validate(draft);
        if (status == PublicationStatus.PUBLISHED && !hasFullContent(draft.content())) {
            throw new MissingContent();
        }
        return toBuilder()
                .categoryId(draft.categoryId()).slug(draft.slug()).title(draft.title())
                .summary(draft.summary()).publicationType(draft.publicationType())
                .accessType(draft.accessType()).creditPrice(draft.creditPrice())
                .content(draft.content()).build();
    }

    /** 只有具备完整正文的内容可以发布；时间由应用用例注入。 */
    public Publication publish(OffsetDateTime now) {
        if (!hasFullContent(content)) {
            throw new MissingContent();
        }
        return toBuilder().status(PublicationStatus.PUBLISHED)
                .deliveryStatus(DeliveryStatus.AVAILABLE).publishedAt(now).build();
    }

    public Publication archive() {
        return toBuilder().status(PublicationStatus.ARCHIVED).build();
    }

    private static boolean hasFullContent(Content content) {
        return content != null && content.fullMarkdown() != null && !content.fullMarkdown().isBlank();
    }

    private static void validate(Draft draft) {
        if (draft.categoryId() <= 0 || draft.publicationType() == null ||
                draft.accessType() == null || draft.content() == null ||
                draft.slug() == null || draft.slug().isBlank() ||
                draft.title() == null || draft.title().isBlank() ||
                draft.summary() == null || draft.summary().isBlank() ||
                draft.creditPrice() < 0 ||
                (draft.accessType() == AccessType.FREE && draft.creditPrice() != 0) ||
                (draft.accessType() == AccessType.CREDIT && draft.creditPrice() == 0)) {
            throw new IllegalArgumentException("发布内容草稿无效");
        }
    }

    /** 草稿、下架或暂停交付时，公开查询一律视为不存在。 */
    public boolean isPubliclyVisible() {
        return status == PublicationStatus.PUBLISHED && deliveryStatus == DeliveryStatus.AVAILABLE;
    }

    /** 当前公开入口只允许免费阅读；积分权益由独立的权益领域判断。 */
    public boolean allowsPublicReading() {
        return isPubliclyVisible() && accessType == AccessType.FREE;
    }
}
