package com.aries.backend.identity.application.port;

import java.time.OffsetDateTime;
import java.util.List;

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
