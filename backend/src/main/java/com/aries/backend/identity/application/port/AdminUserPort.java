package com.aries.backend.identity.application.port;

import java.time.OffsetDateTime;
import java.util.List;

/** 后台成员查询与状态修改端口；管理视图排除密码和其他认证凭据。 */
public interface AdminUserPort {
    record User(
            String id,
            String email,
            String nickname,
            String role,
            String status,
            boolean emailVerified,
            long creditBalance,
            OffsetDateTime createdAt,
            OffsetDateTime lastLoginAt) {}

    List<User> list(String query, String status, int limit, int offset);

    long count(String query, String status);

    User find(long id);

    boolean changeMemberStatus(long id, String status);
}
