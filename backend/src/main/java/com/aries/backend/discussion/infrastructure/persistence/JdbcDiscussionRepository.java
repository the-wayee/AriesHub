package com.aries.backend.discussion.infrastructure.persistence;

import com.aries.backend.discussion.domain.model.Comment;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.discussion.domain.model.DiscussionThread;
import com.aries.backend.discussion.domain.repository.DiscussionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcDiscussionRepository implements DiscussionRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<DiscussionThread> threadRow = (rs, rowNum) -> new DiscussionThread(
            rs.getLong("id"),
            new DiscussionTarget(rs.getString("target_type"), rs.getString("target_key")),
            DiscussionThread.Status.valueOf(rs.getString("status")));

    private final RowMapper<Comment> commentRow = (rs, rowNum) -> new Comment(
            rs.getLong("id"), rs.getLong("thread_id"), rs.getLong("author_id"),
            rs.getObject("parent_id", Long.class), rs.getObject("root_id", Long.class),
            rs.getInt("depth"), rs.getString("body"),
            Comment.Status.valueOf(rs.getString("status")),
            rs.getObject("created_at", java.time.OffsetDateTime.class));

    @Override
    public Optional<DiscussionThread> findThread(DiscussionTarget target) {
        return jdbc.query("""
                SELECT id, target_type, target_key, status
                FROM discussion_threads
                WHERE target_type = ? AND target_key = ? AND is_deleted = false
                """, threadRow, target.type(), target.key()).stream().findFirst();
    }

    @Override
    public DiscussionThread getOrCreateThread(DiscussionTarget target) {
        return jdbc.queryForObject("""
                INSERT INTO discussion_threads(target_type, target_key)
                VALUES (?, ?)
                ON CONFLICT (target_type, target_key) WHERE is_deleted = false
                DO UPDATE SET target_key = EXCLUDED.target_key
                RETURNING id, target_type, target_key, status
                """, threadRow, target.type(), target.key());
    }

    @Override
    public Optional<Comment> findComment(long id) {
        return jdbc.query("""
                SELECT id, thread_id, author_id, parent_id, root_id, depth, body, status, created_at
                FROM comments WHERE id = ? AND is_deleted = false
                """, commentRow, id).stream().findFirst();
    }

    @Override
    public List<Comment> findComments(long threadId) {
        return jdbc.query("""
                SELECT id, thread_id, author_id, parent_id, root_id, depth, body, status, created_at
                FROM comments
                WHERE thread_id = ? AND status = 'PUBLISHED' AND is_deleted = false
                ORDER BY created_at, id
                """, commentRow, threadId);
    }

    @Override
    public Comment save(Comment comment) {
        return jdbc.queryForObject("""
                INSERT INTO comments(thread_id, author_id, parent_id, root_id, depth, body, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id, thread_id, author_id, parent_id, root_id, depth, body, status, created_at
                """, commentRow, comment.threadId(), comment.authorId(), comment.parentId(),
                comment.rootId(), comment.depth(), comment.body(), comment.status().name());
    }
}
