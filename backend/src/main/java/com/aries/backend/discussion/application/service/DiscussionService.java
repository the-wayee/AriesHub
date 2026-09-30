package com.aries.backend.discussion.application.service;

import com.aries.backend.discussion.application.port.DiscussionIdentityProvider;
import com.aries.backend.discussion.application.port.DiscussionReadPort;
import com.aries.backend.discussion.application.port.DiscussionTargetResolver;
import com.aries.backend.discussion.application.query.CommentPageQuery;
import com.aries.backend.discussion.application.view.DiscussionViews.CommentPage;
import com.aries.backend.discussion.application.view.DiscussionViews.CommentView;
import com.aries.backend.discussion.application.view.DiscussionViews.ReplyPage;
import com.aries.backend.discussion.application.view.DiscussionViews.RootCommentView;
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

/**
 * 评论用例。读取按「根评论分页 + 其下回复平铺」组织，写入覆盖发表、回复、点赞和三种审核动作。
 *
 * <p>目标校验按 {@code type} 路由到对应的 {@link DiscussionTargetResolver}：接入新的可评论对象只需
 * 在组合层新增一个实现类，本类不感知任何具体业务模块。
 *
 * <p>{@code like_count} 是反规范化投影，事实以 {@code comment_likes} 为准。计数只做原子自增；
 * 若与真实行数漂移，修复方式是按点赞表重算，而不是继续增量补偿。
 */
@Service
@RequiredArgsConstructor
public class DiscussionService {
    private final DiscussionRepository discussions;
    private final DiscussionReadPort reads;
    private final DiscussionIdentityProvider identities;
    private final List<DiscussionTargetResolver> resolvers;

    /** 按 target type 索引的解析器；首次使用时构建一次，之后只读。 */
    private volatile Map<String, DiscussionTargetResolver> resolverByType;

    @Transactional(readOnly = true)
    public CommentPage comments(DiscussionTarget target, CommentPageQuery query) {
        DiscussionThread thread = discussions.findThread(target).orElse(null);
        // 隐藏的线程对访客表现为空列表而非 404：目标本身仍然存在且可访问。
        if (thread == null || thread.status() == DiscussionThread.Status.HIDDEN) {
            return new CommentPage(List.of(), query.getPage(), query.getSize(), 0, 0);
        }
        long viewer = currentUserIdOrZero();
        long total = reads.countRootComments(thread.id());
        List<RootCommentView> roots = reads.rootComments(thread.id(), viewer, query);
        return new CommentPage(decorateRoots(roots, viewer),
                query.getPage(), query.getSize(), total, pages(total, query.getSize()));
    }

    @Transactional(readOnly = true)
    public ReplyPage replies(long rootId, CommentPageQuery query) {
        long viewer = currentUserIdOrZero();
        long total = reads.countReplies(rootId);
        List<CommentView> items = decorate(reads.replies(rootId, viewer, query), viewer);
        return new ReplyPage(items, query.getPage(), query.getSize(), total, pages(total, query.getSize()));
    }

    @Transactional
    public CommentView add(DiscussionTarget target, Long parentId, String body) {
        long authorId = requireUserId();
        DiscussionThread thread = writableThread(target);
        Comment draft;
        try {
            draft = parentId == null
                    ? Comment.root(thread.id(), authorId, body)
                    : replyTo(thread, parentId, authorId, body);
        } catch (IllegalArgumentException rejected) {
            // 领域规则的拒绝（正文空白/过长、回复已删除的评论）是 4xx，
            // 不能落到 ApiExceptionHandler 的兜底分支变成 500。
            throw new BusinessException(COMMENT_BODY_INVALID);
        }
        Comment saved = discussions.save(draft);
        return decorate(List.of(toView(saved)), authorId).getFirst();
    }

    /** 作者删除自己的评论；保留行与其下回复，避免别人的回复断链。 */
    @Transactional
    public void delete(long commentId) {
        long userId = requireUserId();
        Comment comment = requireComment(commentId);
        if (!comment.authoredBy(userId) && !identities.currentUserIsAdmin()) {
            throw new BusinessException(COMMENT_DELETE_FORBIDDEN);
        }
        discussions.updateStatus(commentId, Comment.Status.DELETED);
    }

    /** 管理员隐藏评论：内容违规时使用，与作者自删区分开。 */
    @Transactional
    public void hide(long commentId) {
        requireComment(commentId);
        discussions.updateStatus(commentId, Comment.Status.HIDDEN);
    }

    /** 管理员锁帖：线程保留可见，但不再接受新的评论和回复。 */
    @Transactional
    public void lockThread(DiscussionTarget target) {
        if (!resolve(target)) throw new BusinessException(DISCUSSION_TARGET_NOT_FOUND);
        discussions.lockThread(target);
    }

    @Transactional
    public boolean toggleLike(long commentId) {
        long userId = requireUserId();
        Comment comment = requireComment(commentId);
        if (comment.status() != Comment.Status.PUBLISHED) {
            throw new BusinessException(COMMENT_NOT_FOUND);
        }
        return discussions.toggleLike(commentId, userId);
    }

    // ---- 内部 ----

    private DiscussionThread writableThread(DiscussionTarget target) {
        if (!resolve(target)) throw new BusinessException(DISCUSSION_TARGET_NOT_FOUND);
        DiscussionThread thread = discussions.getOrCreateThread(target);
        if (!thread.acceptsComments()) throw new BusinessException(DISCUSSION_THREAD_CLOSED);
        return thread;
    }

    private Comment replyTo(DiscussionThread thread, long parentId, long authorId, String body) {
        Comment parent = discussions.findComment(parentId)
                .filter(comment -> comment.threadId() == thread.id())
                .orElseThrow(() -> new BusinessException(COMMENT_NOT_FOUND));
        return Comment.reply(thread.id(), authorId, parent, body);
    }

    private Comment requireComment(long commentId) {
        return discussions.findComment(commentId)
                .orElseThrow(() -> new BusinessException(COMMENT_NOT_FOUND));
    }

    /** 按类型路由目标校验；没有对应解析器时视为目标不存在。 */
    private boolean resolve(DiscussionTarget target) {
        DiscussionTargetResolver resolver = resolverIndex().get(target.type());
        return resolver != null && resolver.exists(target);
    }

    private Map<String, DiscussionTargetResolver> resolverIndex() {
        Map<String, DiscussionTargetResolver> index = resolverByType;
        if (index == null) {
            index = new HashMap<>();
            for (DiscussionTargetResolver resolver : resolvers) {
                index.put(resolver.targetType(), resolver);
            }
            resolverByType = Map.copyOf(index);
        }
        return index;
    }

    /**
     * 未登录访客也能读评论，此时没有「我的点赞」概念，用 0 表示。
     * 匿名由身份端口以 null 表达，不在这里捕获异常。
     */
    private long currentUserIdOrZero() {
        Long userId = identities.currentUserId();
        return userId == null ? 0L : userId;
    }

    /** 写路径要求登录；匿名访问统一返回 401，与全局未登录语义一致。 */
    private long requireUserId() {
        Long userId = identities.currentUserId();
        if (userId == null) throw new BusinessException(COMMENT_LOGIN_REQUIRED);
        return userId;
    }

    /** 一次补齐作者昵称和「能否删除」，避免逐条查询放大成 N 次往返。 */
    private List<CommentView> decorate(List<CommentView> views, long viewer) {
        if (views.isEmpty()) return views;
        Set<Long> authors = new HashSet<>();
        for (CommentView view : views) {
            authors.add(Long.parseLong(view.authorId()));
        }
        Map<Long, String> names = identities.displayNames(authors);
        boolean admin = identities.currentUserIsAdmin();

        List<CommentView> result = new ArrayList<>(views.size());
        for (CommentView view : views) {
            result.add(decorated(view, names, viewer, admin));
        }
        return result;
    }

    /**
     * 根评论连同其预览回复一起补齐昵称：作者集合要跨层合并，
     * 否则每条根评论各查一次，前置的批量查询就白做了。
     */
    private List<RootCommentView> decorateRoots(List<RootCommentView> items, long viewer) {
        if (items.isEmpty()) return items;
        List<CommentView> flat = new ArrayList<>();
        for (RootCommentView item : items) {
            flat.add(item.comment());
            flat.addAll(item.previewReplies());
        }
        Set<Long> authors = new HashSet<>();
        for (CommentView view : flat) {
            authors.add(Long.parseLong(view.authorId()));
        }
        Map<Long, String> names = identities.displayNames(authors);
        boolean admin = identities.currentUserIsAdmin();

        List<RootCommentView> result = new ArrayList<>(items.size());
        for (RootCommentView item : items) {
            List<CommentView> previews = item.previewReplies().stream()
                    .map(reply -> decorated(reply, names, viewer, admin)).toList();
            result.add(new RootCommentView(decorated(item.comment(), names, viewer, admin),
                    item.replyCount(), previews));
        }
        return result;
    }

    private CommentView decorated(CommentView view, Map<Long, String> names, long viewer, boolean admin) {
        long authorId = Long.parseLong(view.authorId());
        boolean canDelete = !view.deleted() && (authorId == viewer || admin);
        return new CommentView(view.id(), view.parentId(), view.rootId(), view.depth(),
                view.authorId(), names.getOrDefault(authorId, "社区成员"), view.body(),
                view.likeCount(), view.likedByMe(), view.deleted(), canDelete, view.createdAt());
    }

    private CommentView toView(Comment comment) {
        return new CommentView(Long.toString(comment.id()),
                comment.parentId() == null ? null : Long.toString(comment.parentId()),
                comment.rootId() == null ? null : Long.toString(comment.rootId()),
                comment.depth(), Long.toString(comment.authorId()), null, comment.visibleBody(),
                comment.likeCount(), false, comment.isDeleted(), false, comment.createdAt());
    }

    private int pages(long total, int size) {
        return (int) ((total + size - 1) / size);
    }
}
