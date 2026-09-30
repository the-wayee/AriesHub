package com.aries.backend.discussion.application.service;

import com.aries.backend.discussion.application.port.DiscussionTargetResolver;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按 targetType 把目标校验路由到对应的 {@link DiscussionTargetResolver}。
 *
 * <p>Spring 会把所有实现类注入进来；启动时建好索引，之后只读。
 * 两个实现声明了同一个类型时直接启动失败，而不是让后注册的悄悄覆盖前一个。
 */
@Component
public class DiscussionTargets {
    private final Map<String, DiscussionTargetResolver> byType;

    public DiscussionTargets(List<DiscussionTargetResolver> resolvers) {
        Map<String, DiscussionTargetResolver> index = new HashMap<>();
        for (DiscussionTargetResolver resolver : resolvers) {
            DiscussionTargetResolver previous = index.putIfAbsent(resolver.targetType(), resolver);
            if (previous != null) {
                throw new IllegalStateException("评论目标类型 " + resolver.targetType() + " 重复注册: "
                        + previous.getClass().getName() + " 与 " + resolver.getClass().getName());
            }
        }
        this.byType = Map.copyOf(index);
    }

    /** 目标存在且可评论；没有对应实现的类型一律视为不存在。 */
    public boolean exists(DiscussionTarget target) {
        DiscussionTargetResolver resolver = byType.get(target.type());
        return resolver != null && resolver.exists(target);
    }
}
