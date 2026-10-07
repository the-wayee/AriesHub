package com.aries.backend.discussion.interfaces.rest.request;

import com.aries.backend.discussion.application.query.CommentPageQuery;
import com.aries.backend.discussion.application.query.CommentPageQuery.Order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/** 评论列表的 HTTP 入参；只承担格式校验，不承载查询逻辑。 默认值在紧凑构造器里补，与 PublicationListRequest 的写法一致。 */
public record CommentListRequest(
        @Min(1) @Max(10000) Integer page,
        @Min(1) @Max(50) Integer size,
        @Pattern(regexp = Order.VALID_VALUES + "|^$") String sort) {

    public CommentListRequest {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
        sort = sort == null ? "" : sort.trim();
    }

    /** HTTP 参数校验结束后，转换成不依赖 Web 框架的应用查询对象。 */
    public CommentPageQuery toQuery() {
        Order order = sort.isEmpty() ? Order.COMPREHENSIVE : Order.valueOf(sort);
        return new CommentPageQuery(page, size, order, 2);
    }
}
