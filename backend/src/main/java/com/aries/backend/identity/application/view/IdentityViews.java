package com.aries.backend.identity.application.view;

import com.aries.backend.identity.domain.model.UserAccount;

import java.time.OffsetDateTime;

/** 身份领域对接口层公开的安全视图。 */
public final class IdentityViews {
    private IdentityViews() {}

    public record CurrentUser(
            String id,
            String email,
            String nickname,
            String bio,
            java.util.UUID avatarFileId,
            String role,
            boolean emailVerified,
            OffsetDateTime createdAt
    ) {
        public static CurrentUser from(UserAccount user) {
            return new CurrentUser(Long.toString(user.getId()), user.getEmail().value(), user.getNickname(),
                    user.getBio(), user.getAvatarFileId(),
                    user.getRole().name(), user.isEmailVerified(), user.getCreatedAt());
        }
    }
}
