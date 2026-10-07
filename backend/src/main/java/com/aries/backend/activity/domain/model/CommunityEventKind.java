package com.aries.backend.activity.domain.model;

/** 社区动态类型；新增类型必须显式接入来源映射与展示，禁止拼接字符串猜测编码。 */
public enum CommunityEventKind {
    MEMBER_JOINED,
    PUBLICATION_PUBLISHED,
    PUBLICATION_LIKE,
    PUBLICATION_BOOKMARK,
    PUBLICATION_SHARE,
    DISCUSSION_COMMENTED,
    DISCUSSION_REPLIED,
    DISCUSSION_LIKED
}
