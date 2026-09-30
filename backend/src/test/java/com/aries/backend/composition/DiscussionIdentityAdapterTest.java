package com.aries.backend.composition;

import com.aries.backend.identity.application.port.SessionManager;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class DiscussionIdentityAdapterTest {

    private record FakeSessions(Optional<Long> current, RuntimeException failure) implements SessionManager {
        @Override public void login(long userId) {}
        @Override public void logout() {}
        @Override public long currentUserId() { throw new UnsupportedOperationException(); }

        @Override
        public Optional<Long> findCurrentUserId() {
            if (failure != null) throw failure;
            return current;
        }
    }

    @Test void anonymousVisitorsHaveNoUserId() {
        var adapter = new DiscussionIdentityAdapter(new FakeSessions(Optional.empty(), null), null);
        assertThat(adapter.currentUserId()).isNull();
        assertThat(adapter.currentUserIsAdmin()).isFalse();
    }

    @Test void loggedInUsersKeepTheirId() {
        var adapter = new DiscussionIdentityAdapter(new FakeSessions(Optional.of(7L), null), null);
        assertThat(adapter.currentUserId()).isEqualTo(7L);
    }

    /** 会话存储故障必须向上抛出，不能被伪装成「未登录」。 */
    @Test void sessionStoreFailuresAreNotTreatedAsAnonymous() {
        var outage = new IllegalStateException("Redis 不可用");
        var adapter = new DiscussionIdentityAdapter(new FakeSessions(Optional.empty(), outage), null);
        assertThatThrownBy(adapter::currentUserId).isSameAs(outage);
    }
}
