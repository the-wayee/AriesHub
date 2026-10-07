package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class DiscussionIntegrationTests extends IntegrationTestSupport {
    private static final String TARGET =
            "/api/v1/discussions/comments" + "?targetType=PUBLICATION&targetKey=free-case";

    @Test
    void commentsArePubliclyReadableButWritingRequiresLogin() throws Exception {
        mvc.perform(get(TARGET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.total").value(0));

        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"这是一条评论"}
                                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMENT_LOGIN_REQUIRED"));
    }

    /** 回复是单层的：回复一条回复仍然落在同一根评论下，深度保持 1。 */
    @Test
    void repliesAreFlatUnderTheirRootComment() throws Exception {
        Cookie member = register("discussion@example.com", "discussion123", "讨论成员");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"根评论"}
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.depth").value(0))
                .andExpect(jsonPath("$.data.authorName").value("讨论成员"))
                .andExpect(jsonPath("$.data.canDelete").value(true));

        Long rootId = database.commentIdByBody("根评论");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case",
                                         "parentId":%d,"body":"回复根评论"}
                                        """
                                                .formatted(rootId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.depth").value(1))
                .andExpect(jsonPath("$.data.parentId").value(rootId.toString()));

        Long replyId = database.commentIdByBody("回复根评论");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case",
                                         "parentId":%d,"body":"回复那条回复"}
                                        """
                                                .formatted(replyId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.depth").value(1))
                .andExpect(jsonPath("$.data.parentId").value(replyId.toString()))
                .andExpect(jsonPath("$.data.rootId").value(rootId.toString()));

        mvc.perform(get(TARGET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].comment.body").value("根评论"))
                .andExpect(jsonPath("$.data.items[0].replyCount").value(2));

        mvc.perform(get("/api/v1/discussions/comments/" + rootId + "/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(
                        jsonPath(
                                "$.data.items[*].depth",
                                org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(1))));
    }

    @Test
    void rootCommentsArePaginatedAndSortable() throws Exception {
        Cookie member = register("paging@example.com", "paging1234", "分页成员");
        long authorId = database.userIdByEmail("paging@example.com");
        database.insertThread("PUBLICATION", "free-case", "OPEN");
        long threadId = database.threadId("PUBLICATION", "free-case");

        // 直接插库，created_at 按索引递减分钟：评论 1 最早，评论 30 最新。
        for (int i = 1; i <= 30; i++) {
            database.insertRootComment(threadId, authorId, "评论 " + i, 30 - i);
        }
        // 让最早发布的一条成为最热，用来区分时间排序和热度排序。
        database.setLikeCount(database.commentIdByBody("评论 1"), 99);

        mvc.perform(get(TARGET + "&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(10)))
                .andExpect(jsonPath("$.data.total").value(30))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        // 最新：评论 30 最新，排在第一页首位。
        mvc.perform(get(TARGET + "&sort=LATEST&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].comment.body").value("评论 30"));
        // 最早的评论落在最后一页的末尾。
        mvc.perform(get(TARGET + "&sort=LATEST&page=3&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(10)))
                .andExpect(jsonPath("$.data.items[9].comment.body").value("评论 1"));

        // 热度：点赞最多的排在最前，与发布时间无关。
        mvc.perform(get(TARGET + "&sort=HOT&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].comment.body").value("评论 1"))
                .andExpect(jsonPath("$.data.items[0].comment.likeCount").value(99));

        // 越界页不截断，返回空 items 和真实 total。
        mvc.perform(get(TARGET + "&page=99&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.total").value(30));
    }

    @Test
    void rootCommentsCarryReplyCountAndPreview() throws Exception {
        Cookie member = register("preview@example.com", "preview123", "预览成员");
        long authorId = database.userIdByEmail("preview@example.com");
        database.insertThread("PUBLICATION", "free-case", "OPEN");
        long threadId = database.threadId("PUBLICATION", "free-case");

        // minutesAgo 越大越早：有回复的这条更新，因此排在最前。
        database.insertRootComment(threadId, authorId, "有回复的根评论", 0);
        database.insertRootComment(threadId, authorId, "没有回复的根评论", 10);
        long rootId = database.commentIdByBody("有回复的根评论");
        for (int i = 1; i <= 5; i++) {
            database.insertReply(threadId, authorId, rootId, "回复 " + i, 6 - i);
        }

        mvc.perform(get(TARGET + "&sort=LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].comment.body").value("有回复的根评论"))
                .andExpect(jsonPath("$.data.items[0].replyCount").value(5))
                // 默认只带两条预览，前端据 replyCount > preview 显示「展开全部」。
                .andExpect(jsonPath("$.data.items[0].previewReplies", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].previewReplies[0].body").value("回复 1"))
                .andExpect(jsonPath("$.data.items[1].comment.body").value("没有回复的根评论"))
                .andExpect(jsonPath("$.data.items[1].replyCount").value(0))
                .andExpect(jsonPath("$.data.items[1].previewReplies", hasSize(0)));
    }

    @Test
    void likesToggleIdempotentlyAndUpdateTheCount() throws Exception {
        Cookie member = register("likes@example.com", "likes1234", "点赞成员");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"值得点赞"}
                                        """))
                .andExpect(status().isCreated());
        Long commentId = database.commentIdByBody("值得点赞");

        mvc.perform(post("/api/v1/discussions/comments/" + commentId + "/like").cookie(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));
        assertThat(database.likeCount(commentId)).isEqualTo(1);
        assertThat(database.activeLikes(commentId)).isEqualTo(1);

        // 再次点击是取消，不是重复点赞。
        mvc.perform(post("/api/v1/discussions/comments/" + commentId + "/like").cookie(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false));
        assertThat(database.likeCount(commentId)).isZero();
        assertThat(database.activeLikes(commentId)).isZero();

        // 取消之后再点赞必须重新生效：点赞行是被逻辑删除的历史行，唯一索引看不到它，
        // 若只靠 INSERT ... ON CONFLICT 会插入重复行，使这次点击变成「取消」。
        mvc.perform(post("/api/v1/discussions/comments/" + commentId + "/like").cookie(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));
        assertThat(database.likeCount(commentId)).isEqualTo(1);
        assertThat(database.activeLikes(commentId)).isEqualTo(1);

        // 必须带登录态：likedByMe 取决于「当前用户」，匿名请求得到的是 false。
        mvc.perform(get(TARGET).cookie(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].comment.likeCount").value(1))
                .andExpect(jsonPath("$.data.items[0].comment.likedByMe").value(true));
    }

    @Test
    void guestsSeeLikesButCannotLike() throws Exception {
        Cookie member = register("guest-like@example.com", "guest1234", "点赞者");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"公开可见"}
                                        """))
                .andExpect(status().isCreated());
        Long commentId = database.commentIdByBody("公开可见");
        mvc.perform(post("/api/v1/discussions/comments/" + commentId + "/like").cookie(member))
                .andExpect(status().isOk());

        mvc.perform(get(TARGET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].comment.likeCount").value(1))
                .andExpect(jsonPath("$.data.items[0].comment.likedByMe").value(false));

        mvc.perform(post("/api/v1/discussions/comments/" + commentId + "/like"))
                .andExpect(status().isUnauthorized());
    }

    /** 作者只能删自己的；删除根评论后其下回复保留，避免别人的发言被一并抹掉。 */
    @Test
    void authorsDeleteTheirOwnCommentsAndRepliesSurvive() throws Exception {
        Cookie author = register("author@example.com", "author1234", "作者");
        Cookie other = register("other@example.com", "other1234", "其他人");

        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(author)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"会被删除"}
                                        """))
                .andExpect(status().isCreated());
        Long rootId = database.commentIdByBody("会被删除");

        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(other)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case",
                                         "parentId":%d,"body":"别人的回复"}
                                        """
                                                .formatted(rootId)))
                .andExpect(status().isCreated());

        // 别人的评论删不掉。
        mvc.perform(delete("/api/v1/discussions/comments/" + rootId).cookie(other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_DELETE_FORBIDDEN"));

        mvc.perform(delete("/api/v1/discussions/comments/" + rootId).cookie(author))
                .andExpect(status().isOk());
        assertThat(database.commentStatus(rootId)).isEqualTo("DELETED");

        // 根评论以「已删除」占位保留，其下别人的回复继续可见：
        // 作者删除自己的发言，不该顺带抹掉别人在他下面的回复。
        mvc.perform(get(TARGET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].comment.deleted").value(true))
                .andExpect(jsonPath("$.data.items[0].comment.body").value(""))
                .andExpect(jsonPath("$.data.items[0].replyCount").value(1))
                .andExpect(jsonPath("$.data.items[0].previewReplies[0].body").value("别人的回复"));
    }

    @Test
    void adminsHideCommentsAndLockThreads() throws Exception {
        Cookie member = register("ordinary@example.com", "ordinary123", "普通成员");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"待治理评论"}
                                        """))
                .andExpect(status().isCreated());
        Long commentId = database.commentIdByBody("待治理评论");

        // 匿名 401 / 普通成员 403 / 管理员放行。
        mvc.perform(post("/api/v1/admin/discussions/comments/" + commentId + "/hide"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/v1/admin/discussions/comments/" + commentId + "/hide")
                                .cookie(member))
                .andExpect(status().isForbidden());

        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        mvc.perform(post("/api/v1/admin/discussions/comments/" + commentId + "/hide").cookie(admin))
                .andExpect(status().isOk());
        assertThat(database.commentStatus(commentId)).isEqualTo("HIDDEN");

        // 隐藏后不再出现在公开列表里。
        mvc.perform(get(TARGET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mvc.perform(
                        post("/api/v1/admin/discussions/threads/lock")
                                .cookie(admin)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case"}
                                        """))
                .andExpect(status().isOk());
        assertThat(database.threadStatus(database.threadId("PUBLICATION", "free-case")))
                .isEqualTo("LOCKED");

        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"锁帖后的评论"}
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DISCUSSION_THREAD_CLOSED"));
    }

    @Test
    void adminsCanLockATargetBeforeItsFirstComment() throws Exception {
        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        assertThat(database.threadId("PUBLICATION", "free-case")).isNull();

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(
                            post("/api/v1/admin/discussions/threads/lock")
                                    .cookie(admin)
                                    .contentType("application/json")
                                    .content(
                                            """
                                            {"targetType":"PUBLICATION","targetKey":"free-case"}
                                            """))
                    .andExpect(status().isOk());
        }
        assertThat(database.threadStatus(database.threadId("PUBLICATION", "free-case")))
                .isEqualTo("LOCKED");
        mvc.perform(get(TARGET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(admin)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"首次评论"}
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DISCUSSION_THREAD_CLOSED"));
    }

    @Test
    void orphanTargetsAndCrossThreadRepliesAreRejected() throws Exception {
        Cookie member = register("boundaries@example.com", "discussion123", "边界成员");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"missing","body":"孤儿评论"}
                                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DISCUSSION_TARGET_NOT_FOUND"));

        // 未注册的目标类型同样被拒绝，而不是落进某个解析器的默认分支。
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"UNKNOWN","targetKey":"whatever","body":"未知目标"}
                                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DISCUSSION_TARGET_NOT_FOUND"));

        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"属于 A"}
                                        """))
                .andExpect(status().isCreated());
        Long rootId = database.commentIdByBody("属于 A");

        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"credit-publication",
                                         "parentId":%d,"body":"错误跨树"}
                                        """
                                                .formatted(rootId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    /** 内容下架或退回草稿后，按原 slug 不能再读出评论，也不能按根评论 id 读出回复。 */
    @Test
    void commentsOfUnpublishedTargetsAreNotReadable() throws Exception {
        Cookie member = register("unpublished@example.com", "unpub1234", "下架测试");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"下架前的评论"}
                                        """))
                .andExpect(status().isCreated());
        Long rootId = database.commentIdByBody("下架前的评论");
        mvc.perform(get("/api/v1/discussions/comments/" + rootId + "/replies"))
                .andExpect(status().isOk());

        database.updatePublicationStatus(11, "ARCHIVED");

        mvc.perform(get(TARGET))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DISCUSSION_TARGET_NOT_FOUND"));
        mvc.perform(get("/api/v1/discussions/comments/" + rootId + "/replies"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        mvc.perform(post("/api/v1/discussions/comments/" + rootId + "/like").cookie(member))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        assertThat(database.likeCount(rootId)).isZero();
        assertThat(database.activeLikes(rootId)).isZero();

        // 草稿内容同理：即使库里已有线程和评论，也读不到。
        long authorId = database.userIdByEmail("unpublished@example.com");
        database.insertThread("PUBLICATION", "draft-case", "OPEN");
        database.insertRootComment(
                database.threadId("PUBLICATION", "draft-case"), authorId, "草稿下的评论", 0);
        mvc.perform(get("/api/v1/discussions/comments?targetType=PUBLICATION&targetKey=draft-case"))
                .andExpect(status().isNotFound());
    }

    /** 被隐藏的根评论，其下回复也不能按 id 单独读出。 */
    @Test
    void repliesOfHiddenRootsAreNotReadable() throws Exception {
        Cookie member = register("hidden-root@example.com", "hidden1234", "隐藏测试");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"将被隐藏的根评论"}
                                        """))
                .andExpect(status().isCreated());
        Long rootId = database.commentIdByBody("将被隐藏的根评论");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case",
                                         "parentId":%d,"body":"它下面的回复"}
                                        """
                                                .formatted(rootId)))
                .andExpect(status().isCreated());

        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        mvc.perform(post("/api/v1/admin/discussions/comments/" + rootId + "/hide").cookie(admin))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/discussions/comments/" + rootId + "/replies"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        // 回复 id 不能当根评论 id 用来绕过检查。
        Long replyId = database.commentIdByBody("它下面的回复");
        mvc.perform(get("/api/v1/discussions/comments/" + replyId + "/replies"))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/discussions/comments/" + replyId + "/like").cookie(member))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        assertThat(database.likeCount(replyId)).isZero();
        assertThat(database.activeLikes(replyId)).isZero();
    }

    @Test
    void likesRespectThreadVisibilityButRemainAvailableInLockedThreads() throws Exception {
        Cookie member = register("thread-likes@example.com", "thread1234", "线程点赞成员");
        long authorId = database.userIdByEmail("thread-likes@example.com");
        database.insertThread("PUBLICATION", "free-case", "LOCKED");
        Long lockedThread = database.threadId("PUBLICATION", "free-case");
        database.insertRootComment(lockedThread, authorId, "锁定线程内的评论", 0);
        Long lockedComment = database.commentIdByBody("锁定线程内的评论");
        mvc.perform(post("/api/v1/discussions/comments/" + lockedComment + "/like").cookie(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));

        database.insertThread("PUBLICATION", "credit-publication", "HIDDEN");
        Long hiddenThread = database.threadId("PUBLICATION", "credit-publication");
        database.insertRootComment(hiddenThread, authorId, "隐藏线程内的评论", 0);
        Long hiddenComment = database.commentIdByBody("隐藏线程内的评论");
        mvc.perform(post("/api/v1/discussions/comments/" + hiddenComment + "/like").cookie(member))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        assertThat(database.likeCount(hiddenComment)).isZero();
        assertThat(database.activeLikes(hiddenComment)).isZero();
    }

    /** 回复已删除的评论是状态冲突，不是正文有问题。 */
    @Test
    void replyingToADeletedCommentIsNotReplyable() throws Exception {
        Cookie member = register("not-replyable@example.com", "reply1234", "回复测试");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"马上删除"}
                                        """))
                .andExpect(status().isCreated());
        Long rootId = database.commentIdByBody("马上删除");
        mvc.perform(delete("/api/v1/discussions/comments/" + rootId).cookie(member))
                .andExpect(status().isOk());

        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case",
                                         "parentId":%d,"body":"正文本身没问题"}
                                        """
                                                .formatted(rootId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_REPLYABLE"));
    }

    /** 领域规则的拒绝是 4xx，不能落到全局兜底变成 500。 */
    @Test
    void invalidCommentBodiesReturnBadRequest() throws Exception {
        Cookie member = register("invalid@example.com", "invalid123", "校验成员");

        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"   "}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(member)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case",
                                         "body":"%s"}
                                        """
                                                .formatted("字".repeat(4001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    /**
     * 回归：审核动作用 {@code update(null, wrapper)} 实现，而 MyBatis-Plus 的 updateFill 只在传入实体时生效——wrapper-only
     * 更新不会自动刷新 updated_at。 仓储必须显式写入它，否则审计时间停在创建时刻，「最近被处理的评论」这类查询会失真。
     */
    @Test
    void moderationActionsRefreshUpdatedAt() throws Exception {
        Cookie author = register("audit@example.com", "audit1234", "审计成员");
        mvc.perform(
                        post("/api/v1/discussions/comments")
                                .cookie(author)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case","body":"审计评论"}
                                        """))
                .andExpect(status().isCreated());
        Long commentId = database.commentIdByBody("审计评论");
        OffsetDateTime commentBefore = database.commentUpdatedAt(commentId);
        OffsetDateTime threadBefore = database.threadUpdatedAt("PUBLICATION", "free-case");
        Thread.sleep(20);

        mvc.perform(delete("/api/v1/discussions/comments/" + commentId).cookie(author))
                .andExpect(status().isOk());
        assertThat(database.commentUpdatedAt(commentId)).isAfter(commentBefore);

        Cookie admin = register("admin@example.com", "admin1234", "管理员");
        mvc.perform(
                        post("/api/v1/admin/discussions/threads/lock")
                                .cookie(admin)
                                .contentType("application/json")
                                .content(
                                        """
                                        {"targetType":"PUBLICATION","targetKey":"free-case"}
                                        """))
                .andExpect(status().isOk());
        assertThat(database.threadUpdatedAt("PUBLICATION", "free-case")).isAfter(threadBefore);
    }

    @Test
    void creditLedgerConstraintsProtectBalanceAndIdempotency() {
        database.insertCreditUser();
        database.insertCreditLedger();

        assertThatThrownBy(database::insertDuplicateCreditLedger)
                .hasMessageContaining("credit_ledger_entries_idempotency_key_key");
        assertThatThrownBy(() -> database.updateCreditBalance(-1))
                .hasMessageContaining("users_credit_balance_check");
    }
}
