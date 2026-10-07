package com.aries.backend.composition;

import cn.dev33.satoken.stp.StpUtil;

import com.aries.backend.catalog.application.port.PublicationReaderIdentity;
import com.aries.backend.identity.application.service.IdentityDirectoryService;
import com.aries.backend.identity.application.service.UserApplicationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/** 所有写操作均验证当前账号仍启用；匿名公开列表不创建会话。 */
@Component
@RequiredArgsConstructor
public class PublicationReaderIdentityAdapter implements PublicationReaderIdentity {
    private final UserApplicationService users;
    private final IdentityDirectoryService directory;

    public Map<Long, String> avatarUrls(Set<Long> ids) {
        return directory.avatarUrls(ids);
    }

    public Map<Long, String> displayNames(Set<Long> ids) {
        return directory.displayNames(ids);
    }

    public long requireUserId() {
        return Long.parseLong(users.currentUser().id());
    }

    public Long optionalUserId() {
        return StpUtil.isLogin() ? requireUserId() : null;
    }
}
