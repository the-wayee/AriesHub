package com.aries.backend.discussion.application.port;

import com.aries.backend.discussion.application.query.CommentPageQuery;
import com.aries.backend.discussion.application.view.DiscussionViews.CommentView;
import com.aries.backend.discussion.application.view.DiscussionViews.RootCommentView;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 评论的只读投影端口。与写入用的 {@code DiscussionRepository} 分开： 列表和预览不需要加载完整聚合，而且「当前用户是否点过赞」这类信息必须随查询一次取回，
 * 不能在应用层逐条查询拼装。
 */
public interface DiscussionReadPort {
    /** 根评论分页；每项带回复总数与前 {@code previewSize} 条回复预览。 */
    List<RootCommentView> rootComments(long threadId, long currentUserId, CommentPageQuery query);

    long countRootComments(long threadId);

    /** 某条根评论下的回复分页，用于「展开全部回复」。 */
    List<CommentView> replies(long rootId, long currentUserId, CommentPageQuery query);

    long countReplies(long rootId);

    /** 批量读取被回复评论的作者；父评论不在当前分页内时也能正确展示 @对象。 */
    Map<String, Long> parentAuthors(Set<Long> parentIds);
}
