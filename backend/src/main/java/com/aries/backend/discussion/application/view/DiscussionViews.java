package com.aries.backend.discussion.application.view;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 评论的只读投影。所有 bigint 标识都以字符串返回，与 catalog 的既有约定一致， 避免前端解析大整数时丢精度。
 *
 * <p>{@code canDelete} 与 {@code likedByMe} 由查询随行返回，前端据此决定是否渲染操作入口， 不必再逐个探测权限。
 */
public final class DiscussionViews {
    private DiscussionViews() {}

    /** 单条评论（根评论或回复）。 */
    public record CommentView(
            String id,
            String parentId,
            String rootId,
            int depth,
            String authorId,
            String authorName,
            String body,
            int likeCount,
            boolean likedByMe,
            boolean deleted,
            boolean canDelete,
            OffsetDateTime createdAt,
            String replyToAuthorName,
            String authorAvatarUrl) {
        /** 兼容不带回复对象的根评论与基础投影；名字统一由应用层批量补齐。 */
        public CommentView(
                String id,
                String parentId,
                String rootId,
                int depth,
                String authorId,
                String authorName,
                String body,
                int likeCount,
                boolean likedByMe,
                boolean deleted,
                boolean canDelete,
                OffsetDateTime createdAt) {
            this(
                    id,
                    parentId,
                    rootId,
                    depth,
                    authorId,
                    authorName,
                    body,
                    likeCount,
                    likedByMe,
                    deleted,
                    canDelete,
                    createdAt,
                    null,
                    null);
        }
    }

    /** 根评论：在 {@link CommentView} 之外附带回复总数与前几条预览。 回复总数是精确值而非估算，用于「共 N 条回复」和是否显示「展开全部」。 */
    public record RootCommentView(
            CommentView comment, long replyCount, List<CommentView> previewReplies) {}

    public record CommentPage(
            List<RootCommentView> items, int page, int size, long total, int totalPages) {}

    public record ReplyPage(
            List<CommentView> items, int page, int size, long total, int totalPages) {}
}
