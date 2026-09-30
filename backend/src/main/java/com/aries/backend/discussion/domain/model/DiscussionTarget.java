package com.aries.backend.discussion.domain.model;

/** 评论挂载目标；业务模块只需提供稳定类型和标识，不与评论表建立多态外键。 */
public record DiscussionTarget(String type, String key) {
    public DiscussionTarget {
        if (type == null || !type.matches("[A-Z][A-Z0-9_]{1,39}") ||
                key == null || key.isBlank() || key.length() > 120) {
            throw new IllegalArgumentException("评论目标无效");
        }
        key = key.trim();
    }
}
