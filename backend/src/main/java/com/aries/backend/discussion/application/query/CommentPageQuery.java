package com.aries.backend.discussion.application.query;

import lombok.Value;

/**
 * 评论分页与排序条件。{@code order} 在 SQL 中必须白名单映射，不能把用户输入直接拼进 ORDER BY。
 *
 * <p>{@code HOT} 与 {@code COMPREHENSIVE} 都按点赞数优先，区别在于综合排序把时间作为较弱的
 * 次级键，让新评论仍有机会上浮；最新排序完全按时间。
 */
@Value
public class CommentPageQuery {
    int page;
    int size;
    Order order;
    /** 每条根评论附带多少条回复预览。 */
    int previewSize;

    public enum Order { LATEST, HOT, COMPREHENSIVE }

    public int getOffset() {
        return (page - 1) * size;
    }

    public boolean isLatest() {
        return order == Order.LATEST;
    }
}
