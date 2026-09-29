package com.aries.backend.identity.infrastructure.persistence.converter;

import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.model.Email;
import com.aries.backend.identity.infrastructure.persistence.po.UserPO;

/** 隔离数据库字段表示与用户领域模型。 */
public final class UserConverter {
    private UserConverter() {}

    public static UserAccount toDomain(UserPO row) {
        return UserAccount.builder()
                .id(row.getId())
                .email(new Email(row.getEmail()))
                .passwordHash(row.getPasswordHash())
                .nickname(row.getNickname())
                .role(UserAccount.Role.valueOf(row.getRole()))
                .status(UserAccount.Status.valueOf(row.getStatus()))
                .emailVerified(Boolean.TRUE.equals(row.getEmailVerified()))
                .lastLoginAt(row.getLastLoginAt())
                .createdAt(row.getCreatedAt())
                .build();
    }

    public static UserPO toPO(UserAccount user) {
        UserPO row = new UserPO();
        if (user.getId() > 0) row.setId(user.getId());
        row.setEmail(user.getEmail().value());
        row.setPasswordHash(user.getPasswordHash());
        row.setNickname(user.getNickname());
        row.setRole(user.getRole().name());
        row.setStatus(user.getStatus().name());
        row.setEmailVerified(user.isEmailVerified());
        row.setLastLoginAt(user.getLastLoginAt());
        return row;
    }
}
