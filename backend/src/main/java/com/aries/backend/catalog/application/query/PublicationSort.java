package com.aries.backend.catalog.application.query;

/** 内容查询排序；向旧查询和 SQL 传递时使用稳定的枚举名称。 */
public enum PublicationSort {
    FEATURED,
    LATEST;

    public static final String VALID_VALUES = "LATEST|FEATURED";
}
