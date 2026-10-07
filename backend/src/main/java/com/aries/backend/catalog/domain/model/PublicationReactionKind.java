package com.aries.backend.catalog.domain.model;

/** 相互独立的阅读者关系；取消关系为物理删除，重新添加产生新的收藏时间。 */
public enum PublicationReactionKind {
    BOOKMARK,
    LIKE
}
