package com.aries.backend.discussion.application.service;

import com.aries.backend.discussion.application.port.DiscussionIdentityProvider;
import com.aries.backend.discussion.application.port.DiscussionTargetResolver;
import com.aries.backend.discussion.application.view.CommentNode;
import com.aries.backend.discussion.domain.model.Comment;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.discussion.domain.model.DiscussionThread;
import com.aries.backend.discussion.domain.repository.DiscussionRepository;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.aries.backend.discussion.application.exception.DiscussionErrorCode.*;

@Service
@RequiredArgsConstructor
public class DiscussionService {
    private final DiscussionRepository discussions;
    private final DiscussionTargetResolver targets;
    private final DiscussionIdentityProvider identities;

    @Transactional(readOnly = true)
    public List<CommentNode> comments(DiscussionTarget target) {
        DiscussionThread thread = discussions.findThread(target).orElse(null);
        if (thread == null || thread.status() == DiscussionThread.Status.HIDDEN) return List.of();
        return tree(discussions.findComments(thread.id()));
    }

    @Transactional
    public CommentNode add(DiscussionTarget target, Long parentId, String body) {
        if (!targets.exists(target)) throw new BusinessException(DISCUSSION_TARGET_NOT_FOUND);
        DiscussionThread thread = discussions.getOrCreateThread(target);
        if (!thread.acceptsComments()) throw new BusinessException(DISCUSSION_THREAD_CLOSED);
        long authorId = identities.currentUserId();
        Comment draft;
        if (parentId == null) {
            draft = Comment.root(thread.id(), authorId, body);
        } else {
            Comment parent = discussions.findComment(parentId)
                    .filter(comment -> comment.threadId() == thread.id())
                    .orElseThrow(() -> new BusinessException(COMMENT_NOT_FOUND));
            try {
                draft = Comment.reply(thread.id(), authorId, parent, body);
            } catch (Comment.ReplyTooDeep error) {
                throw new BusinessException(COMMENT_REPLY_TOO_DEEP);
            }
        }
        Comment saved = discussions.save(draft);
        return node(saved, identities.displayNames(Set.of(authorId)), List.of());
    }

    private List<CommentNode> tree(List<Comment> comments) {
        if (comments.isEmpty()) return List.of();
        Set<Long> authors = new HashSet<>();
        comments.forEach(comment -> authors.add(comment.authorId()));
        Map<Long, String> names = identities.displayNames(authors);
        Map<Long, List<Comment>> children = new HashMap<>();
        for (Comment comment : comments) {
            if (comment.parentId() != null) {
                children.computeIfAbsent(comment.parentId(), ignored -> new ArrayList<>()).add(comment);
            }
        }
        return comments.stream().filter(comment -> comment.parentId() == null)
                .sorted(Comparator.comparing(Comment::createdAt).reversed())
                .map(comment -> nested(comment, children, names)).toList();
    }

    private CommentNode nested(Comment comment, Map<Long, List<Comment>> children,
                               Map<Long, String> names) {
        List<CommentNode> replies = children.getOrDefault(comment.id(), List.of()).stream()
                .sorted(Comparator.comparing(Comment::createdAt))
                .map(child -> nested(child, children, names)).toList();
        return node(comment, names, replies);
    }

    private CommentNode node(Comment comment, Map<Long, String> names, List<CommentNode> replies) {
        return new CommentNode(Long.toString(comment.id()),
                comment.parentId() == null ? null : Long.toString(comment.parentId()),
                comment.depth(), Long.toString(comment.authorId()),
                names.getOrDefault(comment.authorId(), "社区成员"), comment.body(),
                comment.createdAt(), replies);
    }
}
