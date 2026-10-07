package com.aries.backend.composition;

import com.aries.backend.activity.application.port.ActivityContext;
import com.aries.backend.activity.domain.model.CommunityEvent;
import com.aries.backend.activity.domain.model.CommunitySubjectType;
import com.aries.backend.catalog.application.service.CatalogQueryService;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;
import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.discussion.application.service.DiscussionService;
import com.aries.backend.discussion.application.view.DiscussionActivityTarget;
import com.aries.backend.identity.application.service.IdentityDirectoryService;
import com.aries.backend.identity.application.service.UserApplicationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 批量组合各模块的公开能力，不跨领域联表，也不逐条读取作者或文章。 */
@Component
@RequiredArgsConstructor
public class CommunityActivityContextAdapter implements ActivityContext {
    private final UserApplicationService users;
    private final IdentityDirectoryService directory;
    private final CatalogQueryService catalog;
    private final DiscussionService discussion;

    /** 复用身份用例校验登录和账号启用状态，不能仅凭 Cookie 判断已登录。 */
    @Override
    public void requireMember() {
        users.currentUser();
    }

    /** 批量读取公开昵称，缺失账号不伪造作者信息。 */
    @Override
    public Map<Long, String> names(Set<Long> ids) {
        return directory.displayNames(ids);
    }

    /** 批量返回头像签名，避免每条动态单独查询或直接暴露存储对象键。 */
    @Override
    public Map<Long, String> avatars(Set<Long> ids) {
        return directory.avatarUrls(ids);
    }

    /** 把历史事件转换成当前可展示的目标。缺失的事件 ID 表示动态应被过滤， 而不是返回一个可能已经下架或无权访问的链接。 */
    @Override
    public Map<Long, Target> targets(List<CommunityEvent> events) {
        // 先由 discussion 排除已删除/隐藏的评论及隐藏线程，不在组合层复制审核规则。
        Map<Long, DiscussionActivityTarget> comments = visibleComments(events);
        // 文章事件和评论挂载均使用文章 ID，共用一次批量可见性查询。
        Map<Long, PublicationSummary> publications = visiblePublications(events, comments.values());

        Map<Long, Target> result = new HashMap<>();
        for (CommunityEvent event : events) {
            Target target = resolveTarget(event, comments, publications);
            if (target != null) result.put(event.id(), target);
        }
        return result;
    }

    /** 一次查询本批动态涉及的评论，只取当前可见评论的挂载标识和短摘录，不保存历史正文副本。 */
    private Map<Long, DiscussionActivityTarget> visibleComments(List<CommunityEvent> events) {
        Set<Long> commentIds =
                events.stream()
                        .filter(event -> event.subjectType() == CommunitySubjectType.COMMENT)
                        .map(event -> Long.parseLong(event.subjectId()))
                        .collect(Collectors.toSet());
        return discussion.activityTargets(commentIds).stream()
                .collect(Collectors.toMap(DiscussionActivityTarget::commentId, comment -> comment));
    }

    /** 文章事件与评论事件共用一次公开摘要查询，文章/分类下架时由 catalog 排除。 */
    private Map<Long, PublicationSummary> visiblePublications(
            List<CommunityEvent> events, Collection<DiscussionActivityTarget> comments) {
        Set<Long> ids = new HashSet<>();
        for (CommunityEvent event : events) {
            if (event.subjectType() == CommunitySubjectType.PUBLICATION)
                ids.add(Long.parseLong(event.subjectId()));
        }
        for (DiscussionActivityTarget comment : comments) {
            // 当前只接入文章评论；其他业务目标需要补充独立解析，不能误当作文章 ID。
            if (Publication.TARGET_TYPE.equals(comment.targetType()))
                ids.add(Long.parseLong(comment.targetKey()));
        }

        Map<Long, PublicationSummary> result = new HashMap<>();
        for (PublicationSummary publication : catalog.publicSummaries(ids)) {
            // 摘要 ID 在 HTTP 边界用字符串表示，组合层转回数值键，避免精度丢失。
            result.put(Long.parseLong(publication.id()), publication);
        }
        return result;
    }

    /** 按目标类型生成链接；新成员没有已实现的个人主页，因此只展示欢迎动态。 */
    private Target resolveTarget(
            CommunityEvent event,
            Map<Long, DiscussionActivityTarget> comments,
            Map<Long, PublicationSummary> publications) {
        return switch (event.subjectType()) {
            case USER -> new Target("欢迎来到 AriesHub", null, null);
            case PUBLICATION -> publicationTarget(event.subjectId(), false, publications);
            case COMMENT -> {
                DiscussionActivityTarget comment = comments.get(Long.parseLong(event.subjectId()));
                if (comment == null || !Publication.TARGET_TYPE.equals(comment.targetType()))
                    yield null;
                Target publication = publicationTarget(comment.targetKey(), true, publications);
                // 摘录来自当前可见评论，回复显示回复自身内容；文章下架时仍隐藏整条动态。
                yield publication == null
                        ? null
                        : new Target(publication.title(), publication.href(), comment.excerpt());
            }
        };
    }

    /** 公开摘要缺失时隐藏动态；存在时只给阅读入口，付费正文仍由阅读接口校验权益。 */
    private Target publicationTarget(
            String publicationId, boolean comments, Map<Long, PublicationSummary> publications) {
        PublicationSummary publication = publications.get(Long.parseLong(publicationId));
        if (publication == null) return null;
        return new Target(
                publication.title(),
                "/publications/" + publication.id() + (comments ? "#comments" : ""),
                null);
    }
}
