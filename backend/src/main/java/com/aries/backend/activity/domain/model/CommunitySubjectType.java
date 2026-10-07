package com.aries.backend.activity.domain.model;

/** 动态关联对象的稳定编码；领域内使用枚举，数据库边界才转换为字符串。 */
public enum CommunitySubjectType {
    USER,
    PUBLICATION,
    COMMENT
}
