package com.aries.backend.discussion.infrastructure.persistence.mapper;

import com.aries.backend.discussion.application.query.CommentPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 评论的只读投影。分页、预览和「当前用户是否点过赞」都在 SQL 里一次算完：
 * 逐条补查询会把一次页面加载放大成 N 次往返。
 *
 * <p>结果集用扁平行返回、在应用层组装成视图，避免嵌套的 resultMap 配置——
 * 扁平行的字段含义直观，改起来也不会牵动映射文件。
 */
@Mapper
public interface DiscussionReadMapper {

    /**
     * 本页根评论及其回复总数、预览回复。
     *
     * <p>两次查询：先按排序取本页根评论，再用 {@code root_id IN (...)} 一次取回这些根评论的
     * 预览回复。这样回复读取的次数与页码无关，不会随评论数增长。
     */
    List<RootRow> pageRoots(@Param("threadId") long threadId,
                            @Param("userId") long userId,
                            @Param("query") CommentPageQuery query);

    /** 给定根评论下的前 N 条回复，按时间升序。 */
    List<CommentRow> previewReplies(@Param("rootIds") List<Long> rootIds,
                                    @Param("userId") long userId,
                                    @Param("limit") int limit);

    long countRoots(@Param("threadId") long threadId);

    List<CommentRow> pageReplies(@Param("rootId") long rootId,
                                 @Param("userId") long userId,
                                 @Param("query") CommentPageQuery query);

    long countReplies(@Param("rootId") long rootId);

    /** 一条评论的投影行；{@code likedByMe} 由 EXISTS 子查询给出。 */
    record CommentRow(Long id, Long parentId, Long rootId, Integer depth, Long authorId,
                      String body, Integer likeCount, Boolean likedByMe, String status,
                      OffsetDateTime createdAt) {}

    /** 根评论行，附带其回复总数；回复数由 pageRoots 的 LEFT JOIN 直接算出。 */
    record RootRow(Long id, Long authorId, String body, Integer likeCount, Boolean likedByMe,
                   String status, OffsetDateTime createdAt, Long replyCount) {}
}
