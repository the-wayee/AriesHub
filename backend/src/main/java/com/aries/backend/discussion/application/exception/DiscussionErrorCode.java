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
    COMMENT_NOT_FOUND("要操作的评论不存在", NOT_FOUND),
    /** 正文空白或超过 4000 字。 */
    COMMENT_BODY_INVALID("评论内容不符合要求", BAD_REQUEST),
    /** 被回复的评论已隐藏或已删除。与正文校验失败分开，避免用户误以为是自己的内容有问题。 */
    COMMENT_NOT_REPLYABLE("这条评论已不能回复", CONFLICT),
    COMMENT_DELETE_FORBIDDEN("只能删除自己的评论", FORBIDDEN),
    /** 匿名用户尝试写入；由应用层显式拒绝，避免依赖 Sa-Token 在深层抛出。 */
    COMMENT_LOGIN_REQUIRED("请先登录后再参与讨论", UNAUTHORIZED);

    private final String message;
    private final Kind kind;
}
