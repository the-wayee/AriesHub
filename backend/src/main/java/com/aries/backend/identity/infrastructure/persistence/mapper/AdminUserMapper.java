package com.aries.backend.identity.infrastructure.persistence.mapper;

import com.aries.backend.identity.application.port.AdminUserPort.User;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminUserMapper {
    List<User> list(
            @Param("q") String q,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset);

    long count(@Param("q") String q, @Param("status") String status);

    @Select(
            "SELECT id::text AS"
                + " id,email,nickname,role,status,email_verified,credit_balance,created_at,last_login_at"
                + " FROM users WHERE id=#{id} AND is_deleted=false")
    User find(long id);
}
