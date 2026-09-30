package com.aries.backend;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class DiscussionIntegrationTests extends IntegrationTestSupport {
    @Test void commentsArePubliclyReadableButWritingRequiresLogin() throws Exception {
        mvc.perform(get("/api/v1/discussions/comments")
                        .param("targetType", "PUBLICATION").param("targetKey", "free-case"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));

        mvc.perform(post("/api/v1/discussions/comments")
                        .contentType("application/json")
                        .content("""
                                {"targetType":"PUBLICATION","targetKey":"free-case","body":"这是一条评论"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test void repliesAreReturnedAsATreeForAnyRegisteredTarget() throws Exception {
        Cookie member = register("discussion@example.com", "discussion123", "讨论成员");
        mvc.perform(post("/api/v1/discussions/comments").cookie(member)
                        .contentType("application/json")
                        .content("""
                                {"targetType":"PUBLICATION","targetKey":"free-case","body":"根评论"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depth").value(0))
                .andExpect(jsonPath("$.authorName").value("讨论成员"));

        Long rootId = jdbc.queryForObject("SELECT id FROM comments WHERE body = '根评论'", Long.class);
        mvc.perform(post("/api/v1/discussions/comments").cookie(member)
                        .contentType("application/json")
                        .content("""
                                {"targetType":"PUBLICATION","targetKey":"free-case",
                                 "parentId":%d,"body":"回复评论"}
                                """.formatted(rootId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(rootId.toString()))
                .andExpect(jsonPath("$.depth").value(1));

        mvc.perform(get("/api/v1/discussions/comments")
                        .param("targetType", "PUBLICATION").param("targetKey", "free-case"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("根评论"))
                .andExpect(jsonPath("$[0].replies[0].body").value("回复评论"))
                .andExpect(jsonPath("$[0].replies[0].parentId").value(rootId.toString()));
    }

    @Test void orphanTargetsAndCrossThreadRepliesAreRejected() throws Exception {
        Cookie member = register("boundaries@example.com", "discussion123", "边界成员");
        mvc.perform(post("/api/v1/discussions/comments").cookie(member)
                        .contentType("application/json")
                        .content("""
                                {"targetType":"PUBLICATION","targetKey":"missing","body":"孤儿评论"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DISCUSSION_TARGET_NOT_FOUND"));

        mvc.perform(post("/api/v1/discussions/comments").cookie(member)
                        .contentType("application/json")
                        .content("""
                                {"targetType":"PUBLICATION","targetKey":"free-case","body":"属于 A"}
                                """))
                .andExpect(status().isCreated());
        Long rootId = jdbc.queryForObject("SELECT id FROM comments WHERE body = '属于 A'", Long.class);

        mvc.perform(post("/api/v1/discussions/comments").cookie(member)
                        .contentType("application/json")
                        .content("""
                                {"targetType":"PUBLICATION","targetKey":"paid-case",
                                 "parentId":%d,"body":"错误跨树"}
                                """.formatted(rootId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test void creditLedgerConstraintsProtectBalanceAndIdempotency() {
        jdbc.update("INSERT INTO users(email,password_hash,nickname,email_verified) VALUES ('credit@example.com','hash','积分用户',true)");
        jdbc.update("INSERT INTO credit_accounts(user_id,balance) VALUES (1,100)");
        jdbc.update("""
                INSERT INTO credit_ledger_entries(account_id,entry_type,amount,balance_after,
                    reference_type,reference_key,idempotency_key)
                VALUES (1,'GRANT',100,100,'ADMIN_GRANT','welcome','grant:welcome:1')
                """);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO credit_ledger_entries(account_id,entry_type,amount,balance_after,
                    reference_type,reference_key,idempotency_key)
                VALUES (1,'GRANT',100,200,'ADMIN_GRANT','welcome','grant:welcome:1')
                """)).hasMessageContaining("credit_ledger_entries_idempotency_key_key");
        assertThatThrownBy(() -> jdbc.update("UPDATE credit_accounts SET balance = -1 WHERE id = 1"))
                .hasMessageContaining("credit_accounts_balance_check");
    }
}
