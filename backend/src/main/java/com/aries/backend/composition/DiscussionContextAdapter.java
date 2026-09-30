package com.aries.backend.composition;

import com.aries.backend.catalog.domain.repository.CaseRepository;
import com.aries.backend.discussion.application.port.DiscussionIdentityProvider;
import com.aries.backend.discussion.application.port.DiscussionTargetResolver;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.identity.application.port.SessionManager;
import com.aries.backend.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 跨模块组合只发生在 composition：discussion 保持独立，catalog/identity 也无需反向引用它。
 * 后续新增社区动态等目标类型时，在这里增加对应解析器即可。
 */
@Component
@RequiredArgsConstructor
public class DiscussionContextAdapter implements DiscussionTargetResolver, DiscussionIdentityProvider {
    private final CaseRepository cases;
    private final SessionManager sessions;
    private final UserRepository users;

    @Override
    public boolean exists(DiscussionTarget target) {
        if (!"PUBLICATION".equals(target.type())) return false;
        return cases.findBySlug(target.key()).filter(study -> study.isPubliclyVisible()).isPresent();
    }

    @Override
    public long currentUserId() {
        return sessions.currentUserId();
    }

    @Override
    public Map<Long, String> displayNames(Set<Long> userIds) {
        Map<Long, String> result = new HashMap<>();
        for (Long id : userIds) {
            users.findById(id).ifPresent(user -> result.put(id, user.getNickname()));
        }
        return result;
    }
}
