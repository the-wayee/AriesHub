package com.aries.backend.identity.infrastructure.persistence.repository;

import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.model.Email;
import com.aries.backend.identity.domain.repository.UserRepository;
import com.aries.backend.identity.infrastructure.persistence.converter.UserConverter;
import com.aries.backend.identity.infrastructure.persistence.mapper.UserMapper;
import com.aries.backend.identity.infrastructure.persistence.po.UserPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    public Optional<UserAccount> findByEmail(Email email) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<UserPO>lambdaQuery()
                        .eq(UserPO::getEmail, email.value())))
                .map(UserConverter::toDomain);
    }

    @Override
    public Map<Long, String> findNicknamesByIds(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return mapper.selectList(Wrappers.<UserPO>lambdaQuery()
                        .select(UserPO::getId, UserPO::getNickname)
                        .in(UserPO::getId, ids))
                .stream()
                .collect(Collectors.toMap(UserPO::getId, UserPO::getNickname));
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
