package com.aries.backend.catalog.domain.model;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;

/**
 * 案例聚合根：集中表达公开可见和免费阅读规则。
 * 不依赖 Spring、MyBatis-Plus 或 HTTP；正文由单独的受控读取入口提供。
 * 不开放通用 setter，避免后续业务绕过发布、定价等行为直接修改聚合状态。
 */
@Value
@Builder
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

    public enum AccessType { FREE, PAID }
    public enum PublicationStatus { DRAFT, PUBLISHED, ARCHIVED }
    public enum DeliveryStatus { AVAILABLE, SUSPENDED }

    /** 草稿、下架或暂停交付时，公开查询一律视为不存在。 */
    public boolean isPubliclyVisible() {
        return status == PublicationStatus.PUBLISHED && deliveryStatus == DeliveryStatus.AVAILABLE;
    }

    /** M1 只允许免费阅读；后续付费权益由独立的权益领域判断。 */
    public boolean allowsPublicReading() {
        return isPubliclyVisible() && accessType == AccessType.FREE;
    }
}
