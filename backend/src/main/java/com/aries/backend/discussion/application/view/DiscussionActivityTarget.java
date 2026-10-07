package com.aries.backend.discussion.application.view;

/** 当前可见评论的动态投影，携带短摘录；原业务目标仍须验证文章等业务可见性。 */
public record DiscussionActivityTarget(
        long commentId, String targetType, String targetKey, String excerpt) {}
