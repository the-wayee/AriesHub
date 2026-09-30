package com.aries.backend.discussion.domain.model;

/** 一个业务对象对应一个评论线程；锁定或隐藏后禁止继续回复。 */
public record DiscussionThread(long id, DiscussionTarget target, Status status) {
    public enum Status { OPEN, LOCKED, HIDDEN }

    public boolean acceptsComments() {
        return status == Status.OPEN;
    }
}
