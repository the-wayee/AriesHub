package com.aries.backend.discussion.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CommentTest {

    @Test void replyToRootStaysOnTheFirstLevel() {
        Comment root = new Comment(10, 3, 1, null, null, 0, "root",
                Comment.Status.PUBLISHED, 0, 0, null);
        Comment reply = Comment.reply(3, 2, root, "第一条回复");

        assertThat(reply.parentId()).isEqualTo(10);
        assertThat(reply.rootId()).isEqualTo(10);
        assertThat(reply.depth()).isEqualTo(1);
    }

    /**
     * 回复的回复不再增加深度，也不会挂到被回复的那条下面：单层结构里所有回复都平铺在根评论下，
     * parentId 只用来显示「回复 @某人」。这正是根评论分页成立的前提。
     */
    @Test void replyToAReplyStaysFlatUnderTheSameRoot() {
        Comment root = new Comment(10, 3, 1, null, null, 0, "root",
                Comment.Status.PUBLISHED, 0, 0, null);
        Comment first = Comment.reply(3, 2, root, "first");
        Comment savedFirst = new Comment(11, 3, 2, first.parentId(), first.rootId(), first.depth(),
                first.body(), first.status(), 0, 0, null);
        Comment second = Comment.reply(3, 1, savedFirst, "second");

        assertThat(second.parentId()).isEqualTo(11);
        assertThat(second.rootId()).isEqualTo(10);
        assertThat(second.depth()).isEqualTo(1);
    }

    @Test void repliesCannotTargetHiddenOrDeletedComments() {
        for (Comment.Status status : new Comment.Status[]{Comment.Status.HIDDEN, Comment.Status.DELETED}) {
            Comment parent = new Comment(11, 3, 2, 10L, 10L, 1, "被治理的评论", status, 0, 0, null);
            assertThatThrownBy(() -> Comment.reply(3, 1, parent, "尝试回复"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /** 已删除的评论不返回正文，但行本身保留，避免其下的回复断链。 */
    @Test void deletedCommentsHideTheirBody() {
        Comment deleted = new Comment(11, 3, 2, null, null, 0, "原文",
                Comment.Status.DELETED, 0, 0, null);
        assertThat(deleted.visibleBody()).isEmpty();
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.authoredBy(2)).isTrue();
        assertThat(deleted.authoredBy(3)).isFalse();
    }

    @Test void bodyLengthIsCheckedAfterTrimming() {
        Comment root = new Comment(10, 3, 1, null, null, 0, "ok",
                Comment.Status.PUBLISHED, 0, 0, null);
        // 4000 字符加首尾空白：按原文长度会误判为超限，按 trim 后长度应当通过。
        String padded = "  " + "字".repeat(4000) + "  ";
        assertThat(Comment.root(3, 1, padded).body()).hasSize(4000);

        assertThatThrownBy(() -> Comment.root(3, 1, "字".repeat(4001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Comment.root(3, 1, "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Comment.root(3, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
