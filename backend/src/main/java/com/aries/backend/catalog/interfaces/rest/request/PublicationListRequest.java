package com.aries.backend.catalog.interfaces.rest.request;

import com.aries.backend.catalog.application.query.PublicationSearchQuery;
import com.aries.backend.catalog.application.query.PublicationSort;
import com.aries.backend.catalog.domain.model.Publication.AccessType;
import com.aries.backend.catalog.domain.model.Publication.PublicationType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 发布内容列表的 HTTP 入参；只承担格式校验，不承载数据库查询逻辑。 */
public record PublicationListRequest(
        @Min(1) @Max(10000) Integer page,
        @Min(1) @Max(24) Integer size,
        @Size(max = 120) String q,
        @Pattern(regexp = "[a-z0-9-]{0,80}") String category,
        @Pattern(regexp = PublicationType.VALID_VALUES + "|^$") String type,
        @Pattern(regexp = AccessType.VALID_VALUES + "|^$") String access,
        @Pattern(regexp = PublicationSort.VALID_VALUES) String sort,
        Boolean featured) {

    public PublicationListRequest {
        sort = sort == null ? PublicationSort.FEATURED.name() : sort;
        page = page == null ? 1 : page;
        size = size == null ? 9 : size;
        q = q == null ? "" : q.trim();
        category = category == null ? "" : category.trim();
        type = type == null ? "" : type.trim();
        access = access == null ? "" : access.trim();
    }

    /** HTTP 参数校验结束后，转换成不依赖 Web 框架的应用查询对象。 */
    public PublicationSearchQuery toQuery() {
        return new PublicationSearchQuery(page, size, q, category, type, access, sort, featured);
    }
}
