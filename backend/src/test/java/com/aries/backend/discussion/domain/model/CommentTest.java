package com.aries.backend.discussion.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CommentTest {
    @Test void replyKeepsTheRootAndIncrementsDepth() {
        Comment root = new Comment(10, 3, 1, null, null, 0, "root",
                Comment.Status.PUBLISHED, null);
        Comment first = Comment.reply(3, 2, root, "first");
        Comment savedFirst = new Comment(11, 3, 2, first.parentId(), first.rootId(), first.depth(),
                first.body(), first.status(), null);
        Comment second = Comment.reply(3, 1, savedFirst, "second");

        assertThat(first.parentId()).isEqualTo(10);
        assertThat(first.rootId()).isEqualTo(10);
        assertThat(second.parentId()).isEqualTo(11);
        assertThat(second.rootId()).isEqualTo(10);
        assertThat(second.depth()).isEqualTo(2);
    }

    @Test void conversationsCanContinueBeyondFiveReplies() {
        Comment parent = new Comment(20, 3, 1, 19L, 10L, 5,
                "deep", Comment.Status.PUBLISHED, null);
        Comment reply = Comment.reply(3, 2, parent, "continue");
        assertThat(reply.depth()).isEqualTo(6);
        assertThat(reply.parentId()).isEqualTo(20);
        assertThat(reply.rootId()).isEqualTo(10);
    }
}
