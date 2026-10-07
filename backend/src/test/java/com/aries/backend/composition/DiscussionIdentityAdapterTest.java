package com.aries.backend.composition;

import static org.assertj.core.api.Assertions.*;

import com.aries.backend.identity.application.port.SessionManager;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class DiscussionIdentityAdapterTest {

    private record FakeSessions(Optional<Long> current, RuntimeException failure)
            implements SessionManager {
        @Override
        public void login(long userId) {}

        @Override
        public void logout() {}

        @Override
        public void revoke(long userId) {}

        @Override
        public long currentUserId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Long> findCurrentUserId() {
            if (failure != null) throw failure;
            return current;
        }
    }

    @Test
    void anonymousVisitorsHaveNoUserId() {
        DiscussionIdentityAdapter adapter =
                new DiscussionIdentityAdapter(new FakeSessions(Optional.empty(), null), null);
        assertThat(adapter.currentUserId()).isNull();
        assertThat(adapter.currentUserIsAdmin()).isFalse();
    }

    @Test
    void loggedInUsersKeepTheirId() {
        DiscussionIdentityAdapter adapter =
                new DiscussionIdentityAdapter(new FakeSessions(Optional.of(7L), null), null);
        assertThat(adapter.currentUserId()).isEqualTo(7L);
    }

    /** 会话存储故障必须向上抛出，不能被伪装成「未登录」。 */
    @Test
    void sessionStoreFailuresAreNotTreatedAsAnonymous() {
        IllegalStateException outage = new IllegalStateException("Redis 不可用");
        DiscussionIdentityAdapter adapter =
                new DiscussionIdentityAdapter(new FakeSessions(Optional.empty(), outage), null);
        assertThatThrownBy(adapter::currentUserId).isSameAs(outage);
    }
}
