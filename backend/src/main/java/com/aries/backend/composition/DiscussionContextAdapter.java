package com.aries.backend.composition;

import com.aries.backend.catalog.domain.repository.PublicationRepository;
import com.aries.backend.discussion.application.port.DiscussionIdentityProvider;
import com.aries.backend.discussion.application.port.DiscussionTargetResolver;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.identity.application.port.SessionManager;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 跨模块组合只发生在 composition：discussion 保持独立，catalog/identity 也无需反向引用它。
 *
 * <p>实现 {@link DiscussionTargetResolver} 的 {@code supports} 而不是把类型写死在 {@code exists} 里，
 * 是为了让多个目标类型可以并存。若要给社区动态或课程章节加评论，新增一个实现类即可，
 * 本类和 discussion 都不需要改；把类型判断塞进单个 {@code exists} 则在加入第二个实现时
 * 会让 Spring 因存在多个候选 bean 而无法启动。
 *
 * <p>角色判断也只能在这里做：{@code ArchitectureTest} 既禁止 discussion 引用 identity，
 * 也禁止应用层引用 Sa-Token，所以「是否管理员」必须桥接后才交给 discussion。
 */
@Component
@RequiredArgsConstructor
public class DiscussionContextAdapter implements DiscussionTargetResolver, DiscussionIdentityProvider {
    private static final String PUBLICATION = "PUBLICATION";

    private final PublicationRepository publications;
    private final SessionManager sessions;
    private final UserRepository users;

    @Override
    public String targetType() {
        return PUBLICATION;
    }

    @Override
    public boolean exists(DiscussionTarget target) {
        if (!supports(target.type())) return false;
        return publications.findBySlug(target.key())
                .filter(publication -> publication.isPubliclyVisible())
                .isPresent();
    }

    private boolean supports(String type) {
        return PUBLICATION.equals(type);
    }

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

    /** 一次 IN 查询取回全部昵称；逐个 findById 会让评论列表变成 N 次单行查询。 */
    @Override
    public Map<Long, String> displayNames(Set<Long> userIds) {
        return users.findNicknamesByIds(userIds);
    }
}
