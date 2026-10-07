package com.aries.backend.composition;

import cn.dev33.satoken.stp.StpUtil;

import com.aries.backend.catalog.application.port.PublicationReaderIdentity;
import com.aries.backend.identity.application.service.UserApplicationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/** 所有写操作均验证当前账号仍启用；匿名公开列表不创建会话。 */
@Component
@RequiredArgsConstructor
public class PublicationReaderIdentityAdapter implements PublicationReaderIdentity {
    private final UserApplicationService users;

    public long requireUserId() {
        return Long.parseLong(users.currentUser().id());
    }

    public Long optionalUserId() {
        return StpUtil.isLogin() ? requireUserId() : null;
    }
}
