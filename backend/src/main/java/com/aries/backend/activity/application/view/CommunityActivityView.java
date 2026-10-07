package com.aries.backend.activity.application.view;

import com.aries.backend.activity.domain.model.CommunityEventKind;

import java.time.OffsetDateTime;

/** 供首页轮播使用的公开投影；不暴露内部去重键或账号私有信息。 */
public record CommunityActivityView(
        String id,
        String actorId,
        String actorName,
        String actorAvatarUrl,
        CommunityEventKind kind,
        String title,
        String content,
        String href,
        OffsetDateTime createdAt) {}
