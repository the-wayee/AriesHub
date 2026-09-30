package com.aries.backend.discussion.domain.repository;

import com.aries.backend.discussion.domain.model.Comment;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.discussion.domain.model.DiscussionThread;

import java.util.List;
import java.util.Optional;

/** 评论聚合持久化契约，不暴露 SQL 或数据库对象。 */
public interface DiscussionRepository {
    Optional<DiscussionThread> findThread(DiscussionTarget target);
    DiscussionThread getOrCreateThread(DiscussionTarget target);
    Optional<Comment> findComment(long id);
    List<Comment> findComments(long threadId);
    Comment save(Comment comment);
}
