package com.aries.backend;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;
import com.aries.backend.identity.domain.model.VerificationPurpose;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class IdentityIntegrationTests extends IntegrationTestSupport {
    @Autowired SaTokenDao saTokenDao;

    @Test void sessionsArePersistedInRedisInsteadOfProcessMemory() throws Exception {
        assertThat(saTokenDao).isInstanceOf(SaTokenDaoForRedisTemplate.class);
        saTokenDao.set("arieshub:test:satoken-dao", "redis", 60);
        assertThat(redisTemplate.opsForValue().get("arieshub:test:satoken-dao")).isEqualTo("redis");

        Cookie session = register("redis-session@example.com", "redis1234", "Redis 会话");

        mvc.perform(get("/api/v1/users/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("redis-session@example.com"));
    }

    @Test void registrationCreatesSessionAndStoresPasswordHash() throws Exception {
        String code = requestCode("Hello@Example.com", VerificationPurpose.REGISTER);
        var registered = mvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                            {"email":"Hello@Example.com","password":"hello1234","nickname":"小羊","code":"%s"}
                            """.formatted(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.email").value("hello@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("小羊"))
                .andExpect(jsonPath("$.data.emailVerified").value(true))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andReturn();

        String passwordHash = database.passwordHash("hello@example.com");
        assertThat(passwordHash).startsWith("$2").doesNotContain("hello1234");
        assertThat(database.userAuditFieldsPresent(1)).isTrue();

        Cookie session = registered.getResponse().getCookie("arieshub_token");
        assertThat(session).isNotNull();
        mvc.perform(get("/api/v1/users/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("hello@example.com"));
    }

    @Test void duplicateRegistrationAndInvalidLoginReturnStableErrors() throws Exception {
        String code = requestCode("member@example.com", VerificationPurpose.REGISTER);
        String body = """
                {"email":"member@example.com","password":"member123","nickname":"成员","code":"%s"}
                """.formatted(code);
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("""
                        {"email":"member@example.com","password":"wrong-password"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test void loginAndLogoutCompleteTheCookieSessionLifecycle() throws Exception {
        register("login@example.com", "login1234", "登录用户");

        var loggedIn = mvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"email":"LOGIN@example.com","password":"login1234"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("login@example.com"))
                .andReturn();
        Cookie session = loggedIn.getResponse().getCookie("arieshub_token");
        assertThat(session).isNotNull();
        assertThat(database.userHasLastLogin("login@example.com")).isTrue();

        mvc.perform(post("/api/v1/auth/logout").cookie(session))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me").cookie(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test void validationAndLogicalDeleteAreAppliedByFramework() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                        {"email":"bad","password":"short","nickname":"x","code":"12"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        register("delete@example.com", "delete1234", "待删除");
        userMapper.deleteById(1L);
        assertThat(userMapper.selectById(1L)).isNull();
        assertThat(database.userDeleted(1)).isTrue();
    }

    @Test void protectedIdentityEndpointsRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test void verificationCodesAreHashedRateLimitedAndSingleUse() throws Exception {
        String email = "code@example.com";
        String code = requestCode(email, VerificationPurpose.REGISTER);
        mvc.perform(post("/api/v1/auth/email-codes").contentType("application/json").content("""
                        {"email":"code@example.com","purpose":"REGISTER"}
                        """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_TOO_FREQUENT"));

        var keys = redisTemplate.keys("arieshub:auth:code:register:*");
        assertThat(keys).hasSize(1).allMatch(key -> !key.contains(email));
        Object storedHash = redisTemplate.opsForHash().get(keys.iterator().next(), "hash");
        assertThat(storedHash).isNotNull().asString().startsWith("$2").doesNotContain(code);

        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                        {"email":"code@example.com","password":"code12345","nickname":"验证码用户","code":"000000"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_INVALID"));
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                        {"email":"code@example.com","password":"code12345","nickname":"验证码用户","code":"%s"}
                        """.formatted(code)))
                .andExpect(status().isCreated());
        assertThat(redisTemplate.keys("arieshub:auth:code:register:*")).isEmpty();
    }

    @Test void loginIsLimitedByNormalizedEmailAndDoesNotRequireCode() throws Exception {
        register("limit@example.com", "limit1234", "限流用户");
        for (int attempt = 0; attempt < 5; attempt++) {
            mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("""
                            {"email":"LIMIT@example.com","password":"wrong-password"}
                            """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("""
                        {"email":"limit@example.com","password":"limit1234"}
                        """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").isNumber());
        assertThat(redisTemplate.keys("arieshub:auth:rate:login-email:*")).hasSize(1)
                .allMatch(key -> !key.contains("limit@example.com"));
    }

    @Test void malformedAuthRequestsAreLimitedBySourceBeforeValidation() throws Exception {
        for (int attempt = 0; attempt < 60; attempt++) {
            mvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "203.0.113." + attempt)
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "203.0.113.200")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").isNumber());
        mvc.perform(post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.10");
                            return request;
                        })
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        for (int attempt = 0; attempt < 30; attempt++) {
            mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").isNumber());
    }

    @Test void registrationCodesAreOnlyIssuedForRegistrationAndHaveSourceLimit() throws Exception {
        mvc.perform(post("/api/v1/auth/email-codes").contentType("application/json").content("""
                        {"email":"person@example.com","purpose":"LOGIN"}
                        """))
                .andExpect(status().isBadRequest());
        for (int attempt = 1; attempt < 60; attempt++) {
            mvc.perform(post("/api/v1/auth/email-codes").contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/auth/email-codes").contentType("application/json").content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").isNumber());
    }

    @Test void registrationAttemptsAreAlsoLimitedByNormalizedEmail() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                            {"email":"NEW@example.com","password":"member1234","nickname":"新成员","code":"000000"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_EXPIRED"));
        }
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("""
                        {"email":"new@example.com","password":"member1234","nickname":"新成员","code":"000000"}
                        """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").isNumber());
    }
}
