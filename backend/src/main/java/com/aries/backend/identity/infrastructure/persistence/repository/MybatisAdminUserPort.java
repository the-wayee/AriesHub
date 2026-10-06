package com.aries.backend.identity.infrastructure.persistence.repository;

import com.aries.backend.identity.application.port.AdminUserPort;
import com.aries.backend.identity.infrastructure.persistence.mapper.*;
import com.aries.backend.identity.infrastructure.persistence.po.UserPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MybatisAdminUserPort implements AdminUserPort {
    private final AdminUserMapper reads;
    private final UserMapper users;

    public List<User> list(String q, String status, int limit, int offset) {
        return reads.list(q, status, limit, offset);
    }

    public long count(String q, String status) {
        return reads.count(q, status);
    }

    public User find(long id) {
        return reads.find(id);
    }

    public boolean changeMemberStatus(long id, String status) {
        return users.update(
                        null,
                        Wrappers.<UserPO>lambdaUpdate()
                                .eq(UserPO::getId, id)
                                .eq(UserPO::getRole, "USER")
                                .set(UserPO::getStatus, status)
                                .set(UserPO::getUpdatedAt, java.time.OffsetDateTime.now()))
                > 0;
    }
}
