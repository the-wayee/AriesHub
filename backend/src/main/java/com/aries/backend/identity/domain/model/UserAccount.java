package com.aries.backend.identity.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

/** 用户聚合根：保存登录身份及账号状态，不向接口层暴露密码摘要。 */
@Value
@Builder(toBuilder = true)
public class UserAccount {
    long id;
    Email email;
    String passwordHash;
    String nickname;
    @Builder.Default String bio = "";
    java.util.UUID avatarFileId;
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

    /** 资料编辑只改变昵称、签名与头像引用。头像归属由应用端口验证。 */
    public UserAccount editProfile(String name, String signature, java.util.UUID avatar) {
        return toBuilder().nickname(name).bio(signature).avatarFileId(avatar).build();
    }
}
