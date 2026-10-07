package com.aries.backend.discussion.application.event;

/** 评论写入事实只携带标识，评论正文不进入跨模块消息。 */
public record DiscussionActivityOccurred(String eventKey, long actorId, long commentId, Kind kind) {
    public enum Kind {
        COMMENTED,
        REPLIED,
        LIKED
    }
}
