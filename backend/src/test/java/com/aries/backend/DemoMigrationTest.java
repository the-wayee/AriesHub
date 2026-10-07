package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * dev 配置额外加载 {@code db/demo}，其他测试只跑 {@code db/migration}，所以演示数据脚本 原本没有任何测试覆盖：一次改动让它违反了新增的 CHECK
 * 约束，只在本地 dev 启动时才暴露。
 *
 * <p>用独立的数据库容器跑，避免把 demo 迁移写进共享测试库的 Flyway 历史。
 */
class DemoMigrationTest {

    @Test
    void devMigrationsIncludingDemoDataApplyToAFreshDatabase() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            MigrateResult result =
                    Flyway.configure()
                            .dataSource(
                                    postgres.getJdbcUrl(),
                                    postgres.getUsername(),
                                    postgres.getPassword())
                            .locations("classpath:db/migration", "classpath:db/demo")
                            .load()
                            .migrate();

            assertThat(result.success).isTrue();
            assertThat(result.migrations)
                    .anySatisfy(
                            migration ->
                                    assertThat(migration.description).isEqualTo("demo catalog"));
        }
    }
}
