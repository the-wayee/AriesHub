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

        Long rootId = database.commentIdByBody("根评论");
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

    @Test void replyChainsCanGrowBeyondFiveLevels() throws Exception {
        Cookie member = register("deep-replies@example.com", "discussion123", "深层讨论成员");
        mvc.perform(post("/api/v1/discussions/comments").cookie(member)
                        .contentType("application/json")
                        .content("""
                                {"targetType":"PUBLICATION","targetKey":"free-case","body":"第 0 层"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depth").value(0));

        Long parentId = database.commentIdByBody("第 0 层");
        for (int depth = 1; depth <= 6; depth++) {
            String body = "第 " + depth + " 层";
            mvc.perform(post("/api/v1/discussions/comments").cookie(member)
                            .contentType("application/json")
                            .content("""
                                    {"targetType":"PUBLICATION","targetKey":"free-case",
                                     "parentId":%d,"body":"%s"}
                                    """.formatted(parentId, body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.depth").value(depth));
            parentId = database.commentIdByBody(body);
        }

        mvc.perform(get("/api/v1/discussions/comments")
                        .param("targetType", "PUBLICATION").param("targetKey", "free-case"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].replies[0].replies[0].replies[0].replies[0].replies[0].replies[0].depth")
                        .value(6));
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
        Long rootId = database.commentIdByBody("属于 A");

        mvc.perform(post("/api/v1/discussions/comments").cookie(member)
                        .contentType("application/json")
                        .content("""
                                {"targetType":"PUBLICATION","targetKey":"credit-publication",
                                 "parentId":%d,"body":"错误跨树"}
                                """.formatted(rootId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test void creditLedgerConstraintsProtectBalanceAndIdempotency() {
        database.insertCreditUser();
        database.insertCreditAccount();
        database.insertCreditLedger();

        assertThatThrownBy(database::insertDuplicateCreditLedger)
                .hasMessageContaining("credit_ledger_entries_idempotency_key_key");
        assertThatThrownBy(() -> database.updateCreditBalance(-1))
                .hasMessageContaining("credit_accounts_balance_check");
    }
}
