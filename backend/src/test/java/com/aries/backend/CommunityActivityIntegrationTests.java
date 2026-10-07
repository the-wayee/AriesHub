package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aries.backend.activity.application.service.CommunityActivityService;
import com.aries.backend.activity.domain.model.CommunityEventKind;
import com.aries.backend.activity.domain.model.CommunitySubjectType;
import com.aries.backend.activity.domain.repository.CommunityEventRepository;
import com.aries.backend.catalog.application.event.PublicationActivityOccurred;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** 验证跨模块接入、来源事务回滚、去重及删除/下架后的公开可见性。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class CommunityActivityIntegrationTests extends IntegrationTestSupport {
    @Autowired CommunityEventRepository events;
    @Autowired CommunityActivityService activity;
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void joinsReactionsAndCommentsShareOneFeedAndUnavailableTargetsDisappear() throws Exception {
        Cookie member = register("activity@example.com", "reader1234", "新成员");
        mvc.perform(get("/api/v1/community/activities")).andExpect(status().isUnauthorized());
        for (int i = 0; i < 2; i++)
            mvc.perform(put("/api/v1/publications/11/like").cookie(member))
                    .andExpect(status().isOk());
        String commentId =
                JsonPath.read(
                        mvc.perform(
                                        post("/api/v1/discussions/comments")
                                                .cookie(member)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        "{\"targetType\":\"PUBLICATION\",\"targetKey\":\"11\",\"body\":\"一起学习\"}"))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.data.id");
        assertThat(kinds(member))
                .containsExactly("DISCUSSION_COMMENTED", "PUBLICATION_LIKE", "MEMBER_JOINED");
        mvc.perform(delete("/api/v1/discussions/comments/" + commentId).cookie(member))
                .andExpect(status().isOk());
        assertThat(kinds(member)).containsExactly("PUBLICATION_LIKE", "MEMBER_JOINED");
        database.updatePublicationStatus(11, "ARCHIVED");
        assertThat(kinds(member)).containsExactly("MEMBER_JOINED");
        // 不删除历史事实，动态隐藏由业务当前可见性控制。
        assertThat(events.recent(20, null, List.of())).hasSize(3);
    }

    @Test
    void commentAndReplyEventsShowTheirOwnVisibleContent() throws Exception {
        Cookie member = register("excerpt@example.com", "reader1234", "读者");
        String rootResponse =
                mvc.perform(
                                post("/api/v1/discussions/comments")
                                        .cookie(member)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"targetType\":\"PUBLICATION\",\"targetKey\":\"11\",\"body\":\"先梳理需求再动手👏\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String rootId = JsonPath.read(rootResponse, "$.data.id");
        String replyResponse =
                mvc.perform(
                                post("/api/v1/discussions/comments")
                                        .cookie(member)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"targetType\":\"PUBLICATION\",\"targetKey\":\"11\",\"parentId\":"
                                                        + rootId
                                                        + ",\"body\":\"我也试了一次，确实更清楚了。\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String replyId = JsonPath.read(replyResponse, "$.data.id");
        String response =
                mvc.perform(
                                get("/api/v1/community/activities")
                                        .cookie(member)
                                        .param("filter", "COMMENTS"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(JsonPath.<List<String>>read(response, "$.data.items[*].content"))
                .containsExactly("我也试了一次，确实更清楚了。", "先梳理需求再动手👏");
        assertThat(JsonPath.<List<String>>read(response, "$.data.items[*].href"))
                .containsOnly("/publications/11#comments");
        mvc.perform(delete("/api/v1/discussions/comments/" + replyId).cookie(member))
                .andExpect(status().isOk());
        String afterDelete =
                mvc.perform(
                                get("/api/v1/community/activities")
                                        .cookie(member)
                                        .param("filter", "COMMENTS"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(JsonPath.<List<String>>read(afterDelete, "$.data.items[*].content"))
                .containsExactly("先梳理需求再动手👏");
    }

    @Test
    void shareConfirmationAndEventRedeliveryRemainIdempotent() throws Exception {
        Cookie member = register("share-activity@example.com", "reader1234", "分享者");
        String token =
                JsonPath.read(
                        mvc.perform(post("/api/v1/publications/11/share-link").cookie(member))
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.data.token");
        for (int i = 0; i < 3; i++)
            mvc.perform(
                            post("/api/v1/publications/11/share")
                                    .cookie(member)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"token\":\"" + token + "\"}"))
                    .andExpect(status().isOk());
        assertThat(kinds(member)).containsExactly("PUBLICATION_SHARE", "MEMBER_JOINED");
        long actor = events.recent(10, null, List.of()).getFirst().actorId();
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(
                status -> {
                    activity.record(
                            "test-redelivery",
                            actor,
                            CommunityEventKind.PUBLICATION_PUBLISHED,
                            CommunitySubjectType.PUBLICATION,
                            "11");
                    activity.record(
                            "test-redelivery",
                            actor,
                            CommunityEventKind.PUBLICATION_PUBLISHED,
                            CommunitySubjectType.PUBLICATION,
                            "11");
                });
        assertThat(events.recent(10, null, List.of())).hasSize(3);
    }

    @Test
    void failedSourceTransactionLeavesNoCommunityEvent() throws Exception {
        register("rollback-activity@example.com", "reader1234", "成员");
        long actor = events.recent(1, null, List.of()).getFirst().actorId();
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        assertThatThrownBy(
                        () ->
                                transaction.executeWithoutResult(
                                        status -> {
                                            publisher.publishEvent(
                                                    new PublicationActivityOccurred(
                                                            "rolled-back",
                                                            actor,
                                                            11,
                                                            PublicationActivityOccurred.Kind
                                                                    .PUBLISHED));
                                            throw new IllegalStateException("模拟来源事务失败");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(events.recent(10, null, List.of())).hasSize(1);
    }

    /** 游标不受新事件插入影响；筛选必须发生在数据库分页之前。 */
    @Test
    void tabsAndCursorKeepHistoryStableAsNewEventsArrive() throws Exception {
        Cookie member = register("paging@example.com", "reader1234", "读者");
        long actor = events.recent(1, null, List.of()).get(0).actorId();
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(
                status -> {
                    activity.record(
                            "page-like",
                            actor,
                            CommunityEventKind.PUBLICATION_LIKE,
                            CommunitySubjectType.PUBLICATION,
                            "11");
                    activity.record(
                            "page-share",
                            actor,
                            CommunityEventKind.PUBLICATION_SHARE,
                            CommunitySubjectType.PUBLICATION,
                            "11");
                    activity.record(
                            "page-save",
                            actor,
                            CommunityEventKind.PUBLICATION_BOOKMARK,
                            CommunitySubjectType.PUBLICATION,
                            "11");
                });
        String first =
                mvc.perform(
                                get("/api/v1/community/activities")
                                        .cookie(member)
                                        .param("filter", "INTERACTIONS")
                                        .param("size", "1"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(JsonPath.<List<String>>read(first, "$.data.items[*].kind"))
                .containsExactly("PUBLICATION_BOOKMARK");
        String cursor = JsonPath.read(first, "$.data.nextCursor");
        transaction.executeWithoutResult(
                status ->
                        activity.record(
                                "newer-like",
                                actor,
                                CommunityEventKind.PUBLICATION_LIKE,
                                CommunitySubjectType.PUBLICATION,
                                "11"));
        String next =
                mvc.perform(
                                get("/api/v1/community/activities")
                                        .cookie(member)
                                        .param("filter", "INTERACTIONS")
                                        .param("before", cursor))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(JsonPath.<List<String>>read(next, "$.data.items[*].kind"))
                .containsExactly("PUBLICATION_SHARE", "PUBLICATION_LIKE");
        assertThat(JsonPath.<String>read(next, "$.data.nextCursor")).isNull();
        String members =
                mvc.perform(
                                get("/api/v1/community/activities")
                                        .cookie(member)
                                        .param("filter", "MEMBERS"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(JsonPath.<List<String>>read(members, "$.data.items[*].kind"))
                .containsExactly("MEMBER_JOINED");
        mvc.perform(get("/api/v1/community/activities").cookie(member).param("filter", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/community/activities").cookie(member).param("before", "0"))
                .andExpect(status().isBadRequest());
    }

    private List<String> kinds(Cookie member) throws Exception {
        return JsonPath.read(
                mvc.perform(get("/api/v1/community/activities").cookie(member))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.items[*].kind");
    }

    @Test
    void publicationOnlyEmitsOnFirstPublish() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "主理人");
        String id =
                JsonPath.read(
                        mvc.perform(
                                        post("/api/v1/admin/publications")
                                                .cookie(admin)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        """
                                                        {"categoryId":1,"title":"新的实践","summary":"内容摘要",
                                                        "publicationType":"ARTICLE","accessType":"FREE","creditPrice":0,
                                                        "fullMarkdown":"## 开始实践\\n\\n正文"}
                                                        """))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.data.id");
        for (int i = 0; i < 2; i++)
            mvc.perform(post("/api/v1/admin/publications/" + id + "/publish").cookie(admin))
                    .andExpect(status().isOk());
        assertThat(kinds(admin)).containsExactly("PUBLICATION_PUBLISHED", "MEMBER_JOINED");
    }
}
