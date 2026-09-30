package com.aries.backend.discussion.infrastructure.persistence;

import com.aries.backend.discussion.domain.model.Comment;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.discussion.domain.model.DiscussionThread;
import com.aries.backend.discussion.domain.repository.DiscussionRepository;
import com.aries.backend.discussion.infrastructure.persistence.mapper.CommentLikeMapper;
import com.aries.backend.discussion.infrastructure.persistence.mapper.CommentMapper;
import com.aries.backend.discussion.infrastructure.persistence.mapper.DiscussionThreadMapper;
import com.aries.backend.discussion.infrastructure.persistence.po.CommentLikePO;
import com.aries.backend.discussion.infrastructure.persistence.po.CommentPO;
import com.aries.backend.discussion.infrastructure.persistence.po.DiscussionThreadPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 评论聚合的写入仓储。单表查询与增删改用 {@code BaseMapper} 和 Lambda Wrapper 表达，
 * 不用手写 SQL；只有 Wrapper 表达不了的自引用表达式与幂等插入留在 Mapper 注解里。
 */
@Repository
@RequiredArgsConstructor
public class MybatisDiscussionRepository implements DiscussionRepository {
    private final DiscussionThreadMapper threads;
    private final CommentMapper comments;
    private final CommentLikeMapper likes;

    @Override
    public Optional<DiscussionThread> findThread(DiscussionTarget target) {
        return Optional.ofNullable(selectThread(target)).map(this::toDomain);
    }

    @Override
    public Optional<DiscussionThread> findThread(long threadId) {
        return Optional.ofNullable(threads.selectById(threadId)).map(this::toDomain);
    }

    /**
     * 评论区按需创建：内容发布时不建，第一条评论或第一次锁帖时才建。
     *
     * <p>先查后建：评论区绝大多数时候已经存在，常见路径只有一条 SELECT。
     * 查不到时用 {@code ON CONFLICT DO NOTHING} 插入，并发首评由唯一索引仲裁，
     * 再查一次拿到的可能是另一个请求刚建的那一行。
     */
    @Override
    public DiscussionThread getOrCreateThread(DiscussionTarget target) {
        DiscussionThreadPO existing = selectThread(target);
        if (existing != null) return toDomain(existing);
        threads.insertIfAbsent(target.type(), target.key());
        return toDomain(selectThread(target));
    }

    /** 尚无人评论时也允许预先锁帖：复用 getOrCreateThread，创建评论区的逻辑只有一处。 */
    @Override
    public DiscussionThread lockThread(DiscussionTarget target) {
        DiscussionThread thread = getOrCreateThread(target);
        threads.update(null, Wrappers.<DiscussionThreadPO>lambdaUpdate()
                .eq(DiscussionThreadPO::getId, thread.id())
                .set(DiscussionThreadPO::getStatus, DiscussionThread.Status.LOCKED.name())
                // 必须显式写 updated_at：updateFill 只在传入实体时生效，
                // update(null, wrapper) 不会触发自动填充，审计时间会停在旧值。
                .setSql("updated_at = now()"));
        return new DiscussionThread(thread.id(), thread.target(), DiscussionThread.Status.LOCKED);
    }

    @Override
    public Optional<Comment> findComment(long id) {
        return Optional.ofNullable(comments.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<Comment> findReplies(long rootId) {
        return comments.selectList(Wrappers.<CommentPO>lambdaQuery()
                        .eq(CommentPO::getRootId, rootId)
                        .eq(CommentPO::getStatus, Comment.Status.PUBLISHED.name())
                        .orderByAsc(CommentPO::getCreatedAt, CommentPO::getId))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Comment save(Comment comment) {
        CommentPO row = new CommentPO();
        row.setThreadId(comment.threadId());
        row.setAuthorId(comment.authorId());
        row.setParentId(comment.parentId());
        row.setRootId(comment.rootId());
        row.setDepth(comment.depth());
        row.setBody(comment.body());
        row.setStatus(comment.status().name());
        comments.insert(row);
        return toDomain(row);
    }

    @Override
    public void updateStatus(long commentId, Comment.Status status) {
        comments.update(null, Wrappers.<CommentPO>lambdaUpdate()
                .eq(CommentPO::getId, commentId)
                .set(CommentPO::getStatus, status.name())
                // 理由同 lockThread：wrapper-only 更新不触发 MetaObjectHandler 的自动填充。
                .setSql("updated_at = now()"));
    }

    /**
     * 切换点赞状态，返回切换后是否已点赞。
     *
     * <p>取消点赞是<b>物理删除</b>，所以这是一对互斥动作，不需要「复活历史行」：
     * 先尝试删除，删掉一行说明此前已点赞，本次即取消；否则插入，插入成功即点赞。
     *
     * <p>{@code comment_likes} 上 {@code (comment_id, user_id)} 是普通唯一约束，
     * 因此插入用 {@code ON CONFLICT DO NOTHING} 就能同时处理并发重复，
     * 计数紧随其后以原子自增更新。
     */
    @Override
    public boolean toggleLike(long commentId, long userId) {
        int removed = likes.delete(Wrappers.<CommentLikePO>lambdaQuery()
                .eq(CommentLikePO::getCommentId, commentId)
                .eq(CommentLikePO::getUserId, userId));
        if (removed > 0) {
            comments.adjustLikeCount(commentId, -1);
            return false;
        }
        int inserted = comments.insertLikeIfAbsent(commentId, userId);
        if (inserted > 0) {
            comments.adjustLikeCount(commentId, 1);
            return true;
        }
        // 并发下另一次调用刚插入成功：按「已点赞」返回，保证重复请求幂等。
        return true;
    }

    private DiscussionThreadPO selectThread(DiscussionTarget target) {
        return threads.selectOne(Wrappers.<DiscussionThreadPO>lambdaQuery()
                .eq(DiscussionThreadPO::getTargetType, target.type())
                .eq(DiscussionThreadPO::getTargetKey, target.key()));
    }

    private DiscussionThread toDomain(DiscussionThreadPO row) {
        return new DiscussionThread(row.getId(),
                new DiscussionTarget(row.getTargetType(), row.getTargetKey()),
                DiscussionThread.Status.valueOf(row.getStatus()));
    }

    private Comment toDomain(CommentPO row) {
        return new Comment(row.getId(), row.getThreadId(), row.getAuthorId(), row.getParentId(),
                row.getRootId(), row.getDepth(), row.getBody(), Comment.Status.valueOf(row.getStatus()),
                row.getLikeCount() == null ? 0 : row.getLikeCount(), 0, row.getCreatedAt());
    }
}
