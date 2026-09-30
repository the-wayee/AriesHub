package com.aries.backend.composition;

import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.domain.repository.PublicationRepository;
import com.aries.backend.discussion.application.port.DiscussionTargetResolver;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 让发布内容可以被评论：只回答「这个 PUBLICATION 存在且公开吗」。
 *
 * <p>每种可评论对象各有一个这样的类。要给新对象（如社区动态）加评论，
 * 照这个类再写一个、返回新的 {@link #targetType()} 即可，discussion 模块不需要任何改动。
 */
@Component
@RequiredArgsConstructor
public class PublicationDiscussionTarget implements DiscussionTargetResolver {
    private final PublicationRepository publications;

    @Override
    public String targetType() {
        return "PUBLICATION";
    }

    /** 发布内容以 slug 作为 key；草稿、下架、暂停交付的内容不能评论。 */
    @Override
    public boolean exists(DiscussionTarget target) {
        return publications.findBySlug(target.key())
                .filter(Publication::isPubliclyVisible)
                .isPresent();
    }
}
