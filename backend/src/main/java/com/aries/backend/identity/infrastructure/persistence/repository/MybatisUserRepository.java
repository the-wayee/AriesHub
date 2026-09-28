package com.aries.backend.identity.infrastructure.persistence.repository;

import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.repository.UserRepository;
import com.aries.backend.identity.infrastructure.persistence.converter.UserConverter;
import com.aries.backend.identity.infrastructure.persistence.mapper.UserMapper;
import com.aries.backend.identity.infrastructure.persistence.po.UserPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/** 用户仓储的 PostgreSQL 实现。 */
@Repository
@RequiredArgsConstructor
public class MybatisUserRepository implements UserRepository {
    private final UserMapper mapper;

    @Override
    public Optional<UserAccount> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(UserConverter::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<UserPO>lambdaQuery()
                        .eq(UserPO::getEmail, email)))
                .map(UserConverter::toDomain);
    }

    @Override
    public UserAccount save(UserAccount user) {
        UserPO row = UserConverter.toPO(user);
        mapper.insert(row);
        return UserConverter.toDomain(row);
    }

    @Override
    public void updateLastLoginAt(long id, OffsetDateTime loginAt) {
        UserPO row = new UserPO();
        row.setId(id);
        row.setLastLoginAt(loginAt);
        mapper.updateById(row);
    }
}
