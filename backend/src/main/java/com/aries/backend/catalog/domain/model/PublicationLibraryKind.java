package com.aries.backend.catalog.domain.model;

/** 当前用户内容库的筛选类型，默认值和 HTTP 校验共用这一处声明。 */
public enum PublicationLibraryKind {
    BOOKMARK,
    LIKE,
    HISTORY;

    public static final String DEFAULT_CODE = "BOOKMARK";
    public static final String VALID_VALUES = "BOOKMARK|LIKE|HISTORY";
}
