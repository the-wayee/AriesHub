package com.aries.backend.shared.infrastructure.health;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 技术就绪探针：实际查询数据库，不把进程存活误认为业务依赖正常。 */
@Component
@RequiredArgsConstructor
public class DatabaseProbe {
    private final JdbcTemplate jdbc;
    public void verify() { jdbc.queryForObject("SELECT 1", Integer.class); }
}
