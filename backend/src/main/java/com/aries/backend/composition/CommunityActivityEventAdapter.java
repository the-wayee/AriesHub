package com.aries.backend.composition;

import com.aries.backend.activity.application.service.CommunityActivityService;
import com.aries.backend.activity.domain.model.CommunityEventKind;
import com.aries.backend.activity.domain.model.CommunitySubjectType;
import com.aries.backend.catalog.application.event.PublicationActivityOccurred;
import com.aries.backend.discussion.application.event.DiscussionActivityOccurred;
import com.aries.backend.identity.application.event.MemberJoined;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 同步监听并加入来源事务；来源模块只发布自身事实，不依赖动态模块。 */
@Component
@RequiredArgsConstructor
public class CommunityActivityEventAdapter {
    private final CommunityActivityService activity;

    /** 注册成功只产生一条欢迎动态，账号 ID 作为稳定去重键。 */
    @EventListener
    public void joined(MemberJoined event) {
        activity.record(
                "member:" + event.userId(),
                event.userId(),
                CommunityEventKind.MEMBER_JOINED,
                CommunitySubjectType.USER,
                Long.toString(event.userId()));
    }

    /** 显式映射来源枚举，新增文章行为时编译器会要求补齐对应的社区动态类型。 */
    @EventListener
    public void publication(PublicationActivityOccurred event) {
        activity.record(
                event.eventKey(),
                event.actorId(),
                switch (event.kind()) {
                    case LIKE -> CommunityEventKind.PUBLICATION_LIKE;
                    case BOOKMARK -> CommunityEventKind.PUBLICATION_BOOKMARK;
                    case SHARE -> CommunityEventKind.PUBLICATION_SHARE;
                    case PUBLISHED -> CommunityEventKind.PUBLICATION_PUBLISHED;
                },
                CommunitySubjectType.PUBLICATION,
                Long.toString(event.publicationId()));
    }

    /** 只记录评论关联标识，正文和目标当前可见性留给 discussion 读取用例处理。 */
    @EventListener
    public void discussion(DiscussionActivityOccurred event) {
        activity.record(
                event.eventKey(),
                event.actorId(),
                switch (event.kind()) {
                    case COMMENTED -> CommunityEventKind.DISCUSSION_COMMENTED;
                    case REPLIED -> CommunityEventKind.DISCUSSION_REPLIED;
                    case LIKED -> CommunityEventKind.DISCUSSION_LIKED;
                },
                CommunitySubjectType.COMMENT,
                Long.toString(event.commentId()));
    }
}
