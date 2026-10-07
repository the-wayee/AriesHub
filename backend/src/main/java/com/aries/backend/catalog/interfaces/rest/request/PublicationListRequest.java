package com.aries.backend.catalog.interfaces.rest.request;

import com.aries.backend.catalog.application.query.PublicationSearchQuery;

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
        @Pattern(regexp = "CASE_STUDY|ARTICLE|COURSE|^$") String type,
        @Pattern(regexp = "FREE|CREDIT|^$") String access,
        @Pattern(regexp = "LATEST|FEATURED") String sort,
        Boolean featured) {

    public PublicationListRequest {
        sort = sort == null ? "FEATURED" : sort;
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
