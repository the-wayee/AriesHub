package com.aries.backend.discussion.infrastructure.persistence;

import com.aries.backend.discussion.application.port.DiscussionReadPort;
import com.aries.backend.discussion.application.query.CommentPageQuery;
import com.aries.backend.discussion.application.view.DiscussionViews.CommentView;
import com.aries.backend.discussion.application.view.DiscussionViews.RootCommentView;
import com.aries.backend.discussion.infrastructure.persistence.mapper.CommentMapper;
import com.aries.backend.discussion.infrastructure.persistence.mapper.DiscussionReadMapper;
import com.aries.backend.discussion.infrastructure.persistence.mapper.DiscussionReadMapper.CommentRow;
import com.aries.backend.discussion.infrastructure.persistence.mapper.DiscussionReadMapper.RootRow;
import com.aries.backend.discussion.infrastructure.persistence.po.CommentPO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
class MybatisDiscussionReadPort implements DiscussionReadPort {
    private final DiscussionReadMapper reads;
    private final CommentMapper comments;

    @Override
    public Map<String, Long> parentAuthors(Set<Long> parentIds) {
        if (parentIds.isEmpty()) return Map.of();
        Map<String, Long> result = new HashMap<>();
        for (CommentPO parent : comments.selectByIds(parentIds)) {
            // 隐藏评论的作者信息不借由 @回复泄露；自删评论仍保留回复关系。
            if (!"HIDDEN".equals(parent.getStatus()))
                result.put(Long.toString(parent.getId()), parent.getAuthorId());
        }
        return Map.copyOf(result);
    }

    @Override
    public List<RootCommentView> rootComments(
            long threadId, long currentUserId, CommentPageQuery query) {
        List<RootRow> roots = reads.pageRoots(threadId, currentUserId, query);
        if (roots.isEmpty()) return List.of();

        List<Long> rootIds = roots.stream().map(RootRow::id).toList();
        Map<Long, List<CommentView>> previews = new HashMap<>();
        for (CommentRow row :
                reads.previewReplies(rootIds, currentUserId, query.getPreviewSize())) {
            previews.computeIfAbsent(row.rootId(), ignored -> new ArrayList<>()).add(toView(row));
        }

        List<RootCommentView> result = new ArrayList<>(roots.size());
        for (RootRow root : roots) {
            CommentView view =
                    new CommentView(
                            Long.toString(root.id()),
                            null,
                            null,
                            0,
                            Long.toString(root.authorId()),
                            null,
                            visibleBody(root.status(), root.body()),
                            nullToZero(root.likeCount()),
                            Boolean.TRUE.equals(root.likedByMe()),
                            isDeleted(root.status()),
                            false,
                            root.createdAt());
            result.add(
                    new RootCommentView(
                            view,
                            nullToZero(root.replyCount()),
                            previews.getOrDefault(root.id(), List.of())));
        }
        return result;
    }

    @Override
    public long countRootComments(long threadId) {
        return reads.countRoots(threadId);
    }

    @Override
    public List<CommentView> replies(long rootId, long currentUserId, CommentPageQuery query) {
        return reads.pageReplies(rootId, currentUserId, query).stream().map(this::toView).toList();
    }

    @Override
    public long countReplies(long rootId) {
        return reads.countReplies(rootId);
    }

    /**
     * 「已删除」是可见状态，不是过滤条件：作者自删的评论作为占位保留， 正文置空，其下回复继续展示。
     *
     * <p>被删除时同时清空正文和标识，避免把原始内容带出接口；作者与时间保留， 让回复仍能显示「回复 @某人」。
     */
    private String visibleBody(String status, String body) {
        return "DELETED".equals(status) ? "" : body;
    }

    private boolean isDeleted(String status) {
        return "DELETED".equals(status);
    }

    private CommentView toView(CommentRow row) {
        return new CommentView(
                Long.toString(row.id()),
                row.parentId() == null ? null : Long.toString(row.parentId()),
                row.rootId() == null ? null : Long.toString(row.rootId()),
                row.depth() == null ? 0 : row.depth(),
                Long.toString(row.authorId()),
                null,
                visibleBody(row.status(), row.body()),
                nullToZero(row.likeCount()),
                Boolean.TRUE.equals(row.likedByMe()),
                isDeleted(row.status()),
                false,
                row.createdAt());
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }
}
