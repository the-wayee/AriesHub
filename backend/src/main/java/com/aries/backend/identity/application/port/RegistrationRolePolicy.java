package com.aries.backend.identity.application.port;

/** 决定新注册账号是否获得管理员角色，具体名单来源由基础设施层提供。 */
public interface RegistrationRolePolicy {
    boolean isAdmin(String normalizedEmail);
}
