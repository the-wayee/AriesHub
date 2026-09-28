package com.aries.backend.identity.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

/** 用户聚合根：保存登录身份及账号状态，不向接口层暴露密码摘要。 */
@Value
@Builder
public class UserAccount {
    long id;
    String email;
    String passwordHash;
    String nickname;
    Role role;
    Status status;
    boolean emailVerified;
    OffsetDateTime lastLoginAt;
    OffsetDateTime createdAt;

    public enum Role { USER, ADMIN }
    public enum Status { ACTIVE, DISABLED }

    /** 被停用的账号即使密码正确也不能创建登录态。 */
    public boolean canLogin() {
        return status == Status.ACTIVE;
    }
}
