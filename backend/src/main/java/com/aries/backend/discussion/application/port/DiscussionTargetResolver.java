package com.aries.backend.discussion.application.port;

import com.aries.backend.discussion.domain.model.DiscussionTarget;

/** 各内容模块在组合层注册目标校验，防止为不存在或不可见的对象创建孤儿评论。 */
public interface DiscussionTargetResolver {
    boolean exists(DiscussionTarget target);
}
