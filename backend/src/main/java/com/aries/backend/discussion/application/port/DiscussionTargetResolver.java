package com.aries.backend.discussion.application.port;

import com.aries.backend.discussion.domain.model.DiscussionTarget;

/**
 * 各内容模块在组合层注册目标校验，防止为不存在或不可见的对象创建孤儿评论。
 *
 * <p>每个实现声明一个 {@link #targetType()}，discussion 按类型建索引后路由。
 * 这样接入一个新的可评论对象（社区动态、课程章节、作品等）只需新增一个实现类，
 * discussion 与 composition 都不必改动。
 *
 * <p>早期版本把目标类型写死在一个 {@code exists} 里，且按单实现注入。那种形状下加入第二个实现
 * 会让 Spring 因存在多个候选 bean 而无法启动，也无法按类型路由。
 */
public interface DiscussionTargetResolver {
    /** 该解析器负责的目标类型，与 {@link DiscussionTarget#type()} 一致。 */
    String targetType();

    /** 目标是否存在且当前可达（可见/公开）。 */
    boolean exists(DiscussionTarget target);
}
