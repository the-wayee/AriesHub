package com.aries.backend.discussion.domain.model;

import java.time.OffsetDateTime;

/**
 * 评论节点。根评论不保存 rootId；回复保存直接父节点和根节点，既能还原树，
 * 也能按根评论分页后一次读取整棵回复树。
 */
public record Comment(long id, long threadId, long authorId, Long parentId, Long rootId,
                      int depth, String body, Status status, OffsetDateTime createdAt) {
    public enum Status { PUBLISHED, HIDDEN, DELETED }

    public static Comment root(long threadId, long authorId, String body) {
        return new Comment(0, threadId, authorId, null, null, 0,
                normalized(body), Status.PUBLISHED, null);
    }

    public static Comment reply(long threadId, long authorId, Comment parent, String body) {
        if (parent.threadId != threadId || parent.id <= 0 || parent.status != Status.PUBLISHED) {
            throw new IllegalArgumentException("不能回复该评论");
        }
        long root = parent.depth == 0 ? parent.id : parent.rootId;
        return new Comment(0, threadId, authorId, parent.id, root, parent.depth + 1,
                normalized(body), Status.PUBLISHED, null);
    }

    private static String normalized(String body) {
        if (body == null || body.isBlank() || body.trim().length() > 4000) {
            throw new IllegalArgumentException("评论内容无效");
        }
        return body.trim();
    }
}
