package com.aries.backend.discussion.domain.repository;

import com.aries.backend.discussion.domain.model.Comment;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.discussion.domain.model.DiscussionThread;

import java.util.List;
import java.util.Optional;

/**
 * 评论聚合的持久化契约，不暴露 SQL 或数据库对象。只负责写入与按标识加载；
 * 分页和投影查询走 {@code DiscussionReadPort}。
 */
public interface DiscussionRepository {
    Optional<DiscussionThread> findThread(DiscussionTarget target);

    Optional<DiscussionThread> findThread(long threadId);

    DiscussionThread getOrCreateThread(DiscussionTarget target);

    /** 锁定线程，之后不再接受新的评论和回复。 */
    DiscussionThread lockThread(DiscussionTarget target);

    Optional<Comment> findComment(long id);

    /** 整棵回复树，仅用于单条根评论的小范围读取。 */
    List<Comment> findReplies(long rootId);

    Comment save(Comment comment);

    /** 隐藏或删除评论；正文保留，避免其下回复断链。 */
    void updateStatus(long commentId, Comment.Status status);

    /**
     * 切换当前用户对某条评论的点赞状态，返回切换后是否处于已点赞。
     *
     * <p>点赞表是事实来源，{@code comments.like_count} 是投影，两者在同一事务内更新，
     * 且计数使用 {@code like_count = like_count + 1} 形式的原子自增，不做先读后写。
     */
    boolean toggleLike(long commentId, long userId);
}
