package com.aries.backend.identity.application.port;

import java.util.Optional;

/** 登录态端口；应用用例不依赖具体会话框架。 */
public interface SessionManager {
    void login(long userId);

    void logout();

    void revoke(long userId);

    /** 当前登录用户；未登录时抛出未登录异常，由接口层转换为 401。 */
    long currentUserId();

    /**
     * 当前登录用户；未登录时返回空。
     *
     * <p>供「匿名也能访问」的场景使用。只有「没有登录态」会返回空， 会话存储不可用等基础设施故障照常抛出，不能被当成匿名访问。
     */
    Optional<Long> findCurrentUserId();
}
