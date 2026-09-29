package com.aries.backend.identity.application.port;

/** 登录态端口；应用用例不依赖具体会话框架。 */
public interface SessionManager {
    void login(long userId);
    void logout();
    long currentUserId();
}
