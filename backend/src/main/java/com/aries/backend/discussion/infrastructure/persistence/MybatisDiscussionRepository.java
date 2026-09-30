package com.aries.backend.discussion.infrastructure.persistence;

import com.aries.backend.discussion.domain.model.Comment;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.discussion.domain.model.DiscussionThread;
import com.aries.backend.discussion.domain.repository.DiscussionRepository;
import com.aries.backend.discussion.infrastructure.persistence.mapper.CommentMapper;
import com.aries.backend.discussion.infrastructure.persistence.mapper.DiscussionThreadMapper;
import com.aries.backend.discussion.infrastructure.persistence.po.CommentPO;
import com.aries.backend.discussion.infrastructure.persistence.po.DiscussionThreadPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisDiscussionRepository implements DiscussionRepository {
    private final DiscussionThreadMapper threads;
    private final CommentMapper comments;

    @Override
    public Optional<DiscussionThread> findThread(DiscussionTarget target) {
        return Optional.ofNullable(selectThread(target)).map(this::toDomain);
    }

    @Override
    public DiscussionThread getOrCreateThread(DiscussionTarget target) {
        threads.insertIfAbsent(target.type(), target.key());
        return toDomain(selectThread(target));
    }

    @Override
    public Optional<Comment> findComment(long id) {
        return Optional.ofNullable(comments.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<Comment> findComments(long threadId) {
        return comments.selectList(Wrappers.<CommentPO>lambdaQuery()
                        .eq(CommentPO::getThreadId, threadId)
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
                row.getCreatedAt());
    }
}
