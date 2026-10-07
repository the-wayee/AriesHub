package com.aries.backend.discussion.application.port;

import java.util.Map;
import java.util.Set;

/**
 * 由组合层桥接身份模块，discussion 不直接依赖具体认证或用户仓储。
 *
 * <p>这是架构强制的路径而非风格选择：{@code ArchitectureTest} 禁止 {@code ..discussion..} 引用 {@code ..identity..}，也禁止
 * {@code ..application..} 引用 {@code cn.dev33.satoken..}， 所以登录态和角色判断只能经由这里跨模块传递。
 */
public interface DiscussionIdentityProvider {

    /**
     * 当前登录用户；匿名访问时返回 {@code null}。
     *
     * <p>用返回 null 而不是抛异常来表达匿名：评论是公开可读的，匿名是正常状态而非错误路径， 让调用方在常规流程里捕获异常会把匿名访问写成一连串 try/catch。
     */
    Long currentUserId();

    /** 当前登录用户是否为管理员；匿名或用户不存在时为 false。 */
    boolean currentUserIsAdmin();

    /** 批量取昵称，避免按作者逐个查询。 */
    /** 批量获取头像签名，未设置头像的账号不出现在映射中。 */
    Map<Long, String> avatarUrls(Set<Long> ids);

    Map<Long, String> displayNames(Set<Long> userIds);
}
