package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** 验证开发库整理保留评论、回复、点赞和审核状态，既有 ID 线程不能被重复创建。 */
class PublicationIdMigrationTest {
    @Test
    void migrationMergesThreadsWithoutLosingCommentsOrModeration() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .target("19")
                    .load()
                    .migrate();
            try (Connection connection =
                            DriverManager.getConnection(
                                    postgres.getJdbcUrl(),
                                    postgres.getUsername(),
                                    postgres.getPassword());
                    Statement sql = connection.createStatement()) {
                sql.execute("INSERT INTO categories(id,slug,name) VALUES(1,'coding','编程')");
                sql.execute(
                        "INSERT INTO users(id,email,password_hash,nickname)"
                                + " VALUES(1,'reader@example.com','hash','读者')");
                sql.execute(
                        "INSERT INTO publications(id,category_id,slug,title,summary,access_type)"
                            + " VALUES(11,1,'old-name','文章','摘要','FREE'),(12,1,'second-name','第二篇','摘要','FREE')");
                sql.execute(
                        "INSERT INTO discussion_threads(id,target_type,target_key,status)"
                            + " VALUES(1,'PUBLICATION','old-name','HIDDEN'),(2,'PUBLICATION','11','OPEN'),(3,'PUBLICATION','second-name','LOCKED'),(4,'PUBLICATION','unrelated','OPEN')");
                sql.execute(
                        "INSERT INTO comments(id,thread_id,author_id,body)"
                                + " VALUES(1,1,1,'根评论'),(3,2,1,'现有 ID 评论')");
                sql.execute(
                        "INSERT INTO comments(id,thread_id,author_id,parent_id,root_id,depth,body)"
                                + " VALUES(2,1,1,1,1,1,'回复')");
                sql.execute("INSERT INTO comment_likes(comment_id,user_id) VALUES(2,1)");

                Flyway.configure()
                        .dataSource(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword())
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();
                try (ResultSet rows =
                        sql.executeQuery(
                                "SELECT thread_id,parent_id,root_id FROM comments WHERE id=2")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong("thread_id")).isEqualTo(2);
                    assertThat(rows.getLong("parent_id")).isEqualTo(1);
                    assertThat(rows.getLong("root_id")).isEqualTo(1);
                }
                try (ResultSet rows =
                        sql.executeQuery(
                                "SELECT status FROM discussion_threads WHERE target_key='11' AND"
                                        + " NOT is_deleted")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("status")).isEqualTo("HIDDEN");
                    assertThat(rows.next()).isFalse();
                }
                try (ResultSet rows =
                        sql.executeQuery(
                                "SELECT target_key,status FROM discussion_threads WHERE id=3")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("target_key")).isEqualTo("12");
                    assertThat(rows.getString("status")).isEqualTo("LOCKED");
                }
                try (ResultSet rows =
                        sql.executeQuery("SELECT count(*) FROM comment_likes WHERE comment_id=2")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(1);
                }
                try (ResultSet rows =
                        sql.executeQuery(
                                "SELECT count(*) FROM information_schema.columns WHERE"
                                        + " table_name='publications' AND column_name='slug'")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isZero();
                }
            }
        }
    }
}
