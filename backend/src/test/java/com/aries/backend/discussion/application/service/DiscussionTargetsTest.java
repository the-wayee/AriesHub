package com.aries.backend.discussion.application.service;

import static org.assertj.core.api.Assertions.*;

import com.aries.backend.discussion.application.port.DiscussionTargetResolver;
import com.aries.backend.discussion.domain.model.DiscussionTarget;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class DiscussionTargetsTest {

    /** 测试用解析器：只认给定的 key。 */
    private record FakeResolver(String targetType, Set<String> keys)
            implements DiscussionTargetResolver {
        @Override
        public boolean exists(DiscussionTarget target) {
            return keys.contains(target.key());
        }
    }

    @Test
    void routesEachTypeToItsOwnResolver() {
        DiscussionTargets targets =
                new DiscussionTargets(
                        List.of(
                                new FakeResolver("PUBLICATION", Set.of("ai-ppt-outline")),
                                new FakeResolver("POST", Set.of("42"))));

        assertThat(targets.exists(new DiscussionTarget("PUBLICATION", "ai-ppt-outline"))).isTrue();
        assertThat(targets.exists(new DiscussionTarget("POST", "42"))).isTrue();
        // key 只在对应类型的解析器里查，不会串到别的类型。
        assertThat(targets.exists(new DiscussionTarget("POST", "ai-ppt-outline"))).isFalse();
    }

    @Test
    void unknownTypesAreTreatedAsMissing() {
        DiscussionTargets targets =
                new DiscussionTargets(List.of(new FakeResolver("PUBLICATION", Set.of("a"))));
        assertThat(targets.exists(new DiscussionTarget("UNKNOWN", "a"))).isFalse();
    }

    @Test
    void duplicateTypesFailAtStartup() {
        assertThatThrownBy(
                        () ->
                                new DiscussionTargets(
                                        List.of(
                                                new FakeResolver("POST", Set.of()),
                                                new FakeResolver("POST", Set.of()))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POST");
    }
}
