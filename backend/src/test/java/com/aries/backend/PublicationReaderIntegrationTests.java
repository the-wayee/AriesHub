package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** 通过实际 PostgreSQL/Redis 与 HTTP 验证幂等、隔离、状态变化和阅读版本。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class PublicationReaderIntegrationTests extends IntegrationTestSupport {
    @Test
    void shareLinksUseConfiguredOriginAndRejectUnpublishedArticles() throws Exception {
        Cookie member = register("links@example.com", "reader1234", "林舟");
        mvc.perform(post("/api/v1/publications/11/share-link").cookie(member))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.url")
                                .value(
                                        org.hamcrest.Matchers.startsWith(
                                                "http://localhost:3200/s/")));
        mvc.perform(post("/api/v1/publications/13/share-link").cookie(member))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventCountsAreIdempotentAndFeedExcludesViewsAndUnpublishedContent() throws Exception {
        Cookie member = register("events@example.com", "reader1234", "林舟");
        String token = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        String shared =
                JsonPath.read(
                        mvc.perform(post("/api/v1/publications/11/share-link").cookie(member))
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.data.token");
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/v1/publications/11/view")
                                    .cookie(member)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"token\":\"" + token + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.viewCount").value(1));
            mvc.perform(
                            post("/api/v1/publications/11/share")
                                    .cookie(member)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"token\":\"" + shared + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.shareCount").value(1));
            mvc.perform(put("/api/v1/publications/11/like").cookie(member));
            mvc.perform(put("/api/v1/publications/11/bookmark").cookie(member));
        }
        mvc.perform(get("/api/v1/home/activity").cookie(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].actorName").value("林舟"));
        mvc.perform(get("/api/v1/home/activity")).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/v1/publications/11/share")
                                .cookie(member)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/v1/publications/13/view")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/api/v1/publications/11/view")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(2));
        database.updatePublicationStatus(11, "ARCHIVED");
        mvc.perform(get("/api/v1/home/activity").cookie(member))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void reactionsAreIdempotentPrivateAndFilteredWhenArchived() throws Exception {
        Cookie one = register("reader-one@example.com", "reader1234", "成员一");
        Cookie two = register("reader-two@example.com", "reader1234", "成员二");
        mvc.perform(put("/api/v1/publications/11/like")).andExpect(status().isUnauthorized());
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/v1/publications/11/like").cookie(one))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.likeCount").value(1));
            mvc.perform(put("/api/v1/publications/11/bookmark").cookie(one))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.bookmarked").value(true));
        }
        mvc.perform(get("/api/v1/users/me/library").cookie(one))
                .andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/v1/users/me/library").cookie(two))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/v1/publications/11/interaction").cookie(two))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.bookmarked").value(false));
        mvc.perform(get("/api/v1/publications/11/interaction"))
                .andExpect(jsonPath("$.data.liked").value(false));
        for (int i = 0; i < 2; i++)
            mvc.perform(delete("/api/v1/publications/11/like").cookie(one))
                    .andExpect(jsonPath("$.data.likeCount").value(0));
        database.updatePublicationStatus(11, "ARCHIVED");
        mvc.perform(get("/api/v1/users/me/library").cookie(one))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(put("/api/v1/publications/11/bookmark").cookie(two))
                .andExpect(status().isNotFound());
    }

    @Test
    void concurrentRepeatedLikesNeverCreateDuplicateRows() throws Exception {
        Cookie member = register("parallel-reader@example.com", "reader1234", "并发成员");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> tasks = new java.util.ArrayList<Future<Integer>>();
            for (int i = 0; i < 8; i++)
                tasks.add(
                        executor.submit(
                                () ->
                                        mvc.perform(
                                                        put("/api/v1/publications/11/like")
                                                                .cookie(member))
                                                .andReturn()
                                                .getResponse()
                                                .getStatus()));
            for (Future<Integer> task : tasks)
                assertThat(task.get(20, TimeUnit.SECONDS)).isEqualTo(200);
        }
        mvc.perform(get("/api/v1/publications/11/interaction").cookie(member))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    void readingHistoryRequiresFreeContentAndCurrentVersionAndIsAccountScoped() throws Exception {
        Cookie one = register("reading-one@example.com", "reader1234", "成员一");
        Cookie two = register("reading-two@example.com", "reader1234", "成员二");
        mvc.perform(get("/api/v1/publications/11/reading-progress").cookie(one))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(
                        put("/api/v1/publications/11/reading-progress")
                                .cookie(one)
                                .contentType("application/json")
                                .content(
                                        "{\"version\":\"1.0\",\"position\":\"read-intro-0\",\"percent\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percent").value(42));
        mvc.perform(get("/api/v1/users/me/library").param("kind", "HISTORY").cookie(one))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].progress.position").value("read-intro-0"));
        mvc.perform(get("/api/v1/users/me/library").param("kind", "HISTORY").cookie(two))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(
                        put("/api/v1/publications/12/reading-progress")
                                .cookie(one)
                                .contentType("application/json")
                                .content("{\"version\":\"1.0\",\"position\":\"\",\"percent\":20}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONTENT_LOCKED"));
        mvc.perform(
                        put("/api/v1/publications/11/reading-progress")
                                .cookie(one)
                                .contentType("application/json")
                                .content("{\"version\":\"old\",\"position\":\"\",\"percent\":20}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("READING_VERSION_CHANGED"));
        mvc.perform(
                        put("/api/v1/publications/11/reading-progress")
                                .cookie(one)
                                .contentType("application/json")
                                .content("{\"version\":\"1.0\",\"position\":\"\",\"percent\":101}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/home").cookie(one))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.continueReading[0].publication.id").value("11"));
        // 缺少进度不得被 Java 基本类型默认成 0%，覆盖与 OpenAPI 必填字段的一致性。
        mvc.perform(
                        put("/api/v1/publications/11/reading-progress")
                                .cookie(one)
                                .contentType("application/json")
                                .content("{\"version\":\"1.0\",\"position\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/home")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicCardQueriesFilterSortAndNeverContainPaidBodies() throws Exception {
        mvc.perform(get("/api/v1/publications/cards").param("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].publication.id").value("12"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "SECRET_SENTINEL"))));
        mvc.perform(get("/api/v1/publications/cards").param("access", "FREE"))
                .andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/v1/publications/cards").param("featured", "true"))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/v1/publications/cards").param("sort", "BROKEN"))
                .andExpect(status().isBadRequest());
    }
}
