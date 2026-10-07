package com.aries.backend.identity.domain.repository;

import com.aries.backend.identity.domain.model.Email;
import com.aries.backend.identity.domain.model.UserAccount;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** 用户聚合仓储契约，领域和应用层不依赖 MyBatis-Plus。 */
public interface UserRepository {
    Optional<UserAccount> findById(long id);

    Optional<UserAccount> findByEmail(Email email);

    /**
     * 批量取昵称。存在的用户才会出现在返回的映射里；调用方按缺失键回退到默认显示名。 提供这个批量入口是因为逐个 {@link #findById} 会让一次评论列表加载变成 N 次单行查询。
     */
    Map<Long, String> findNicknamesByIds(Set<Long> ids);

    /** 批量读取公开头像引用；不返回账号的私有字段。 */
    Map<Long, UUID> findAvatarIdsByIds(Set<Long> ids);

    UserAccount save(UserAccount user);

    void updateProfile(UserAccount user);

    void updateLastLoginAt(long id, OffsetDateTime loginAt);
}
