package com.aries.backend.discussion.domain.model;

import java.time.OffsetDateTime;

/**
 * 评论节点。
 *
 * <p>回复结构是<b>单层</b>的：一条评论要么是根评论（{@code depth = 0}），要么是挂在某个根评论下的回复
 * （{@code depth = 1}）。回复的回复仍然是 {@code depth = 1}，通过 {@code parentId} 指向被回复的那条，
 * 界面据此显示「回复 @某人」，而不是继续嵌套。
 *
 * <p>这样根评论可以独立分页，其下回复按 {@code rootId} 一次取回，前端不必处理任意深度的树。
 * 早期版本采用无限嵌套，递归组装深度等于回复链长度，是用户可以构造的故障；改单层后这个风险消失。
 *
 * <p>{@code likeCount} 与 {@code replyCount} 是反规范化计数，用于在分页之前排序（热门/综合）。
 * 真实事实以 {@code comment_likes} 和评论行本身为准；计数漂移时用重算修复，不做增量补偿。
 */
public record Comment(long id, long threadId, long authorId, Long parentId, Long rootId,
                      int depth, String body, Status status,
                      int likeCount, int replyCount, OffsetDateTime createdAt) {
    public enum Status { PUBLISHED, HIDDEN, DELETED }

    /** 根评论。 */
    public static Comment root(long threadId, long authorId, String body) {
        return new Comment(0, threadId, authorId, null, null, 0,
                normalized(body), Status.PUBLISHED, 0, 0, null);
    }

    /**
     * 回复。无论回复的是根评论还是另一条回复，都落在同一个根评论下：
     * 直接挂在根评论上时 {@code parentId} 即根评论 id，回复某条回复时 {@code parentId} 是那条回复，
     * 但 {@code rootId} 始终指向共同的根。
     */
    public static Comment reply(long threadId, long authorId, Comment parent, String body) {
        if (parent.threadId != threadId || parent.id <= 0 || parent.status != Status.PUBLISHED) {
            throw new IllegalArgumentException("不能回复该评论");
        }
        long root = parent.depth == 0 ? parent.id : parent.rootId;
        return new Comment(0, threadId, authorId, parent.id, root, 1,
                normalized(body), Status.PUBLISHED, 0, 0, null);
    }

    public Comment withStatus(Status next) {
        return new Comment(id, threadId, authorId, parentId, rootId, depth, body, next,
                likeCount, replyCount, createdAt);
    }

    public boolean isDeleted() {
        return status == Status.DELETED;
    }

    public boolean authoredBy(long userId) {
        return authorId == userId;
    }

    /**
     * 已删除的评论不再对外返回正文，但行本身保留：它下面可能有别人的回复，
     * 父行被物理删除会让回复断链。正文为空表示「该评论已删除」。
     */
    public String visibleBody() {
        return status == Status.DELETED ? "" : body;
    }

    private static String normalized(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        String trimmed = body.trim();
        if (trimmed.length() > 4000) {
            throw new IllegalArgumentException("评论内容过长");
        }
        return trimmed;
    }
}
