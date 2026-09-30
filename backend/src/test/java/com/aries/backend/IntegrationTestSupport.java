package com.aries.backend;

import com.aries.backend.catalog.infrastructure.persistence.mapper.CatalogReadMapper;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import com.aries.backend.identity.infrastructure.persistence.mapper.UserMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 跨模块集成测试共用的容器、数据和邮件假实现。 */
abstract class IntegrationTestSupport {
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        // 容器测试在高负载 CI 中容许更长响应；不改变生产的 2 秒超时。
        registry.add("spring.data.redis.timeout", () -> "10s");
        registry.add("app.security.admin-emails", () -> "admin@example.com");
    }

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.aries.backend.storage.application.port.ObjectStorage objects;

    @Autowired MockMvc mvc;
    @Autowired TestDataMapper database;
    @Autowired CatalogReadMapper mapper;
    @Autowired UserMapper userMapper;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired FakeEmailCodeSender emailSender;

    @BeforeEach
    void fixtures() {
        database.reset();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        emailSender.clear();
        database.insertCategories();
        insert(11, 1, "free-case", "免费 AI 工具", "FREE", "PUBLISHED", "AVAILABLE", "FREE_BODY");
        insert(12, 2, "credit-publication", "积分 PPT 实战", "CREDIT", "PUBLISHED", "AVAILABLE", "CREDIT_SECRET_SENTINEL");
        insert(13, 1, "draft-case", "草稿", "FREE", "DRAFT", "AVAILABLE", "DRAFT_SECRET_SENTINEL");
        insert(14, 1, "archived-case", "下架", "FREE", "ARCHIVED", "AVAILABLE", "ARCHIVED_SECRET_SENTINEL");
        insert(15, 1, "suspended-case", "停用", "FREE", "PUBLISHED", "SUSPENDED", "SUSPENDED_SECRET_SENTINEL");
    }

    String requestCode(String email, VerificationPurpose purpose) throws Exception {
        mvc.perform(post("/api/v1/auth/email-codes")
                        .contentType("application/json")
                        .content("""
                            {"email":"%s","purpose":"%s"}
                            """.formatted(email, purpose.name())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.expiresInSeconds").value(600))
                .andExpect(jsonPath("$.resendAfterSeconds").value(60));
        return emailSender.latest(email.toLowerCase(), purpose);
    }

    Cookie register(String email, String password, String nickname) throws Exception {
        String code = requestCode(email, VerificationPurpose.REGISTER);
        var response = mvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                            {"email":"%s","password":"%s","nickname":"%s","code":"%s"}
                            """.formatted(email, password, nickname, code)))
                .andExpect(status().isCreated())
                .andReturn();
        return response.getResponse().getCookie("arieshub_token");
    }

    void insert(long id, int category, String slug, String title, String access,
                        String status, String delivery, String content) {
        long creditPrice = "CREDIT".equals(access) ? 199 : 0;
        database.insertPublication(id, category, slug, title, access, creditPrice, status, delivery);
        database.insertPublicationContent(id, content);
    }

}
