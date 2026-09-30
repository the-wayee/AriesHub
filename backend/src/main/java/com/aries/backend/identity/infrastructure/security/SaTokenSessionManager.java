package com.aries.backend.identity.infrastructure.security;

import cn.dev33.satoken.stp.StpUtil;
import com.aries.backend.identity.application.port.SessionManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

    /** Sa-Token 在无 token、token 无效或已过期时返回 null；读取 Redis 失败仍会抛异常。 */
    @Override
    public Optional<Long> findCurrentUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? Optional.empty() : Optional.of(Long.parseLong(loginId.toString()));
    }
}
