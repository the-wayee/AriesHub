package com.aries.backend.discussion.application.port;

import java.util.Map;
import java.util.Set;

/** 由组合层桥接身份模块，discussion 不直接依赖具体认证或用户仓储。 */
public interface DiscussionIdentityProvider {
    long currentUserId();
    Map<Long, String> displayNames(Set<Long> userIds);
}
