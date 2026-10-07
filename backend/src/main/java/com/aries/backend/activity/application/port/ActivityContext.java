package com.aries.backend.activity.application.port;

import com.aries.backend.activity.domain.model.CommunityEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 身份和目标可见性经组合层提供，动态领域不访问其他领域的数据库。 */
public interface ActivityContext {
    /** 验证当前访问者是可用的登录成员；认证或存储故障由原身份用例处理。 */
    void requireMember();

    /** 一次补齐公开昵称；不存在的账号不包含在映射中。 */
    Map<Long, String> names(Set<Long> ids);

    /** 已验证文件归属的头像签名，没有头像的成员省略该键。 */
    Map<Long, String> avatars(Set<Long> ids);

    /** 以事件 ID 为键；原业务目标不可见时省略，调用方据此隐藏该条动态。 */
    Map<Long, Target> targets(List<CommunityEvent> events);

    /** href 为站内入口，null 表示无跳转；content 为可见评论摘录，其他事件为 null。 */
    record Target(String title, String href, String content) {}
}
