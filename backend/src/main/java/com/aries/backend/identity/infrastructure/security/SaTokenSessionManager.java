package com.aries.backend.identity.infrastructure.security;

import cn.dev33.satoken.stp.StpUtil;
import com.aries.backend.identity.application.port.SessionManager;
import org.springframework.stereotype.Component;

/** 用 Sa-Token 实现应用层会话端口。 */
@Component
public class SaTokenSessionManager implements SessionManager {
    @Override
    public void login(long userId) {
        StpUtil.login(userId);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
