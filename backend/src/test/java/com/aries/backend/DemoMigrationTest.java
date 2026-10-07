package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * dev 配置额外加载 {@code db/demo}，其他测试只跑 {@code db/migration}，所以演示数据脚本 原本没有任何测试覆盖：一次改动让它违反了新增的 CHECK
 * 约束，只在本地 dev 启动时才暴露。
 *
 * <p>用独立的数据库容器跑，避免把 demo 迁移写进共享测试库的 Flyway 历史。
 */
class DemoMigrationTest {

    @Test
    void devMigrationsIncludingDemoDataApplyToAFreshDatabase() throws Exception {
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
            try (Connection connection =
                            DriverManager.getConnection(
                                    postgres.getJdbcUrl(),
                                    postgres.getUsername(),
                                    postgres.getPassword());
                    Statement statement = connection.createStatement()) {
                try (ResultSet rows =
                        statement.executeQuery(
                                "SELECT count(*), count(DISTINCT kind) FROM community_events WHERE"
                                        + " event_key LIKE 'demo-community:%'")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt(1)).isEqualTo(16);
                    assertThat(rows.getInt(2)).isEqualTo(8);
                }
                // 开发用户不可登录；再次执行种子脚本不会重复评论、点赞或动态。
                statement.execute(
                        "INSERT INTO users(id,email,password_hash,nickname,status)"
                            + " VALUES(10000,'sequence-test@demo.invalid','unused','序列校验','DISABLED')");
                statement.execute("SELECT setval(pg_get_serial_sequence('users','id'),1,false)");
                statement.execute(
                        Files.readString(
                                Path.of(
                                        "src/main/resources/db/demo/R__demo_community_activity.sql")));
                try (ResultSet rows =
                        statement.executeQuery(
                                "SELECT count(*) FROM community_events WHERE event_key LIKE"
                                        + " 'demo-community:%'")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt(1)).isEqualTo(16);
                }
                try (ResultSet rows =
                        statement.executeQuery(
                                "SELECT count(*) FROM users WHERE email LIKE"
                                        + " '%@demo.arieshub.invalid' AND status = 'DISABLED'")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt(1)).isEqualTo(4);
                }
                try (ResultSet rows = statement.executeQuery("SELECT count(*) FROM comments")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt(1)).isEqualTo(2);
                }
            }
        }
    }
}
