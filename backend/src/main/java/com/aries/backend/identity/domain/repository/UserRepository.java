package com.aries.backend.identity.domain.repository;

import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.model.Email;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 用户聚合仓储契约，领域和应用层不依赖 MyBatis-Plus。 */
public interface UserRepository {
    Optional<UserAccount> findById(long id);
    Optional<UserAccount> findByEmail(Email email);

    /**
     * 批量取昵称。存在的用户才会出现在返回的映射里；调用方按缺失键回退到默认显示名。
     * 提供这个批量入口是因为逐个 {@link #findById} 会让一次评论列表加载变成 N 次单行查询。
     */
    Map<Long, String> findNicknamesByIds(Set<Long> ids);

    UserAccount save(UserAccount user);
    void updateLastLoginAt(long id, OffsetDateTime loginAt);
}
