package com.aries.backend.identity.infrastructure.persistence.repository;

import com.aries.backend.identity.application.port.AdminUserPort;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.infrastructure.persistence.mapper.UserMapper;
import com.aries.backend.identity.infrastructure.persistence.po.UserPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

/** 后台成员单表查询及状态更新；只选择公开管理字段，不查询密码摘要。 */
@Repository
@RequiredArgsConstructor
public class MybatisAdminUserPort implements AdminUserPort {
    private final UserMapper users;

    @Override
    public List<User> list(String query, String status, int limit, int offset) {
        Page<UserPO> page = new Page<UserPO>(offset / limit + 1L, limit, false);
        return users
                .selectPage(
                        page,
                        visibleColumns(query(query, status))
                                .orderByDesc(UserPO::getCreatedAt, UserPO::getId))
                .getRecords()
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public long count(String query, String status) {
        return users.selectCount(query(query, status));
    }

    @Override
    public User find(long id) {
        UserPO row =
                users.selectOne(
                        visibleColumns(Wrappers.<UserPO>lambdaQuery()).eq(UserPO::getId, id));
        return row == null ? null : toView(row);
    }

    /** 只允许修改普通成员的状态；显式更新时间，不覆盖资料、密码或并发余额。 */
    @Override
    public boolean changeMemberStatus(long id, String status) {
        return users.update(
                        null,
                        Wrappers.<UserPO>lambdaUpdate()
                                .eq(UserPO::getId, id)
                                .eq(UserPO::getRole, UserAccount.Role.USER.name())
                                .set(UserPO::getStatus, status)
                                .setSql("updated_at = now()"))
                > 0;
    }

    private LambdaQueryWrapper<UserPO> visibleColumns(LambdaQueryWrapper<UserPO> query) {
        return query.select(
                UserPO::getId,
                UserPO::getEmail,
                UserPO::getNickname,
                UserPO::getRole,
                UserPO::getStatus,
                UserPO::getEmailVerified,
                UserPO::getCreditBalance,
                UserPO::getCreatedAt,
                UserPO::getLastLoginAt);
    }

    private LambdaQueryWrapper<UserPO> query(String query, String status) {
        LambdaQueryWrapper<UserPO> wrapper = Wrappers.<UserPO>lambdaQuery();
        if (!query.isEmpty()) {
            // Lambda like 无法表达 PostgreSQL 的大小写不敏感搜索；固定表达式使用参数绑定。
            // 用户输入的 !、% 和 _ 是普通字符，不允许扩大查询范围。
            String keyword =
                    "%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            wrapper.apply("(email ILIKE {0} ESCAPE '!' OR nickname ILIKE {0} ESCAPE '!')", keyword);
        }
        return wrapper.eq(!status.isEmpty(), UserPO::getStatus, status);
    }

    private User toView(UserPO row) {
        return new User(
                row.getId().toString(),
                row.getEmail(),
                row.getNickname(),
                row.getRole(),
                row.getStatus(),
                Boolean.TRUE.equals(row.getEmailVerified()),
                row.getCreditBalance(),
                row.getCreatedAt(),
                row.getLastLoginAt());
    }
}
