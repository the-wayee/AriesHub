package com.aries.backend.activity.domain.model;

import java.time.OffsetDateTime;

/** 社区行为事实；只保存关联标识，不复制正文、邮件或文件下载地址。 */
public record CommunityEvent(
        Long id,
        String eventKey,
        long actorId,
        CommunityEventKind kind,
        CommunitySubjectType subjectType,
        String subjectId,
        OffsetDateTime createdAt) {}
