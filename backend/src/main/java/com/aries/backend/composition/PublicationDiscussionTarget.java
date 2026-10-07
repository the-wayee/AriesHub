package com.aries.backend.composition;

import com.aries.backend.catalog.application.service.CatalogQueryService;
import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.discussion.application.port.DiscussionTargetResolver;
import com.aries.backend.discussion.domain.model.DiscussionTarget;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * 让发布内容可以被评论：只回答「这个 PUBLICATION 存在且公开吗」。
 *
 * <p>每种可评论对象各有一个这样的类。要给新对象（如社区动态）加评论， 照这个类再写一个、返回新的 {@link #targetType()} 即可，discussion 模块不需要任何改动。
 */
@Component
@RequiredArgsConstructor
public class PublicationDiscussionTarget implements DiscussionTargetResolver {
    private final CatalogQueryService publications;

    @Override
    public String targetType() {
        return Publication.TARGET_TYPE;
    }

    /** 文章评论只接受正整数 ID；非法标识视为目标不存在，草稿、下架、暂停交付均不能评论。 */
    @Override
    public boolean exists(DiscussionTarget target) {
        if (!target.key().matches("[1-9][0-9]{0,18}")) return false;
        try {
            return publications.isPubliclyVisibleById(Long.parseLong(target.key()));
        } catch (NumberFormatException invalidId) {
            // 超出 bigint 的输入来自外部请求，不让格式错误变成服务器异常。
            return false;
        }
    }
}
