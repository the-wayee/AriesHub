package com.aries.backend.composition;

import com.aries.backend.discussion.application.port.DiscussionIdentityProvider;
import com.aries.backend.identity.application.port.SessionManager;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 把身份模块的登录态、角色和昵称提供给 discussion。
 *
 * <p>必须放在 composition：{@code ArchitectureTest} 禁止 discussion 引用 identity，
 * 也禁止应用层引用 Sa-Token，所以这些信息只能经这里桥接。
 */
@Component
@RequiredArgsConstructor
public class DiscussionIdentityAdapter implements DiscussionIdentityProvider {
    private final SessionManager sessions;
    private final UserRepository users;

    /** 匿名访问返回 null；Sa-Token 在未登录时抛异常，这里把它收敛成「没有登录态」。 */
    @Override
    public Long currentUserId() {
        try {
            return sessions.currentUserId();
        } catch (RuntimeException notLoggedIn) {
            return null;
        }
    }

    @Override
    public boolean currentUserIsAdmin() {
        Long userId = currentUserId();
        if (userId == null) return false;
        return users.findById(userId)
                .map(user -> user.getRole() == UserAccount.Role.ADMIN)
                .orElse(false);
    }

    /** 一次 IN 查询取回全部昵称，避免评论列表变成 N 次单行查询。 */
    @Override
    public Map<Long, String> displayNames(Set<Long> userIds) {
        return users.findNicknamesByIds(userIds);
    }
}
