package com.aries.backend.composition;

import com.aries.backend.identity.application.port.SessionManager;
import com.aries.backend.storage.application.port.StorageIdentityProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageIdentityAdapter implements StorageIdentityProvider {
    private final SessionManager sessions;
    @Override public long currentUserId() { return sessions.currentUserId(); }
}
