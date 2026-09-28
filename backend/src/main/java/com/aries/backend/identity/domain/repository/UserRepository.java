package com.aries.backend.identity.domain.repository;

import com.aries.backend.identity.domain.model.UserAccount;

import java.time.OffsetDateTime;
import java.util.Optional;

/** 用户聚合仓储契约，领域和应用层不依赖 MyBatis-Plus。 */
public interface UserRepository {
    Optional<UserAccount> findById(long id);
    Optional<UserAccount> findByEmail(String email);
    UserAccount save(UserAccount user);
    void updateLastLoginAt(long id, OffsetDateTime loginAt);
}
