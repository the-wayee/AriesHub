package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Activity;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Interaction;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Progress;
import com.aries.backend.catalog.domain.model.PublicationEventKind;
import com.aries.backend.catalog.domain.model.PublicationReactionKind;

import java.util.List;
import java.util.Map;

/** 互动写入采用 MyBatis-Plus，个人列表与批量计数由只读联表投影提供。 */
public interface PublicationReaderRepository {
    /** 返回是否新增事件，供用例只在首次确认时发布社区动态。 */
    boolean event(Long userId, long publicationId, PublicationEventKind kind, String dedupKey);

    /** 返回文章互动投影；这里只含文章行为，全社区动态使用 activity 模块。 */
    List<Activity> activity(int size);

    /** 是否存在当前账号与指定文章的有效解锁权益，不从积分余额推断。 */
    boolean unlocked(long userId, long publicationId);

    /** 写入互动前锁定公开文章行，防止并发下架后仍新增互动；不可见返回 false。 */
    boolean lockVisible(long publicationId);

    /** 返回是否真正新增关系；取消和重复提交返回 false，但取消仍会删除关系。 */
    boolean reaction(
            long userId, long publicationId, PublicationReactionKind kind, boolean enabled);

    /** 按文章 ID 批量补齐计数；userId 为 null 时只返回总数，个人关系为未选中。 */
    Map<String, Interaction> interactions(List<Long> ids, Long userId);

    /** 按账号和文章 ID 返回有效阅读位置；过期正文版本不能恢复旧位置。 */
    Map<String, Progress> progress(List<Long> ids, long userId);

    /** 保存当前位置；调用方先验证正文版本与阅读权益，不能用客户端版本绕过校验。 */
    void saveProgress(
            long userId, long publicationId, String version, String position, int percent);

    /** 按个人收藏、点赞或历史类型分页读取当前仍公开的文章。 */
    List<PublicationSummary> library(long userId, String kind, int page, int size);

    /** 与 library 使用相同的个人关系和公开条件，确保分页总数一致。 */
    long libraryCount(long userId, String kind);
}
