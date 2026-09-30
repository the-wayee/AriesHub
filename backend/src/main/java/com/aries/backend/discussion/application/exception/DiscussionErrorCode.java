package com.aries.backend.discussion.application.exception;

import com.aries.backend.shared.application.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.aries.backend.shared.application.exception.ErrorCode.Kind.*;

@Getter
@RequiredArgsConstructor
public enum DiscussionErrorCode implements ErrorCode {
    DISCUSSION_TARGET_NOT_FOUND("评论对象不存在或尚未公开", NOT_FOUND),
    DISCUSSION_THREAD_CLOSED("当前讨论已关闭", CONFLICT),
    COMMENT_NOT_FOUND("要回复的评论不存在", NOT_FOUND),
    COMMENT_REPLY_TOO_DEEP("回复层级已达到上限", BAD_REQUEST);

    private final String message;
    private final Kind kind;
}
