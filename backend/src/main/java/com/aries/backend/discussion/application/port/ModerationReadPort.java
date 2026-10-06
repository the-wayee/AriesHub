package com.aries.backend.discussion.application.port;

import java.time.OffsetDateTime;
import java.util.List;

/** 管理员审核只读投影；作者已删除的评论正文必须隐藏。 */
public interface ModerationReadPort {
    record Comment(
            String id,
            String authorName,
            String targetType,
            String targetKey,
            String title,
            String body,
            String status,
            String threadStatus,
            OffsetDateTime createdAt) {}

    List<Comment> list(String status, int limit, int offset);

    long count(String status);
}
