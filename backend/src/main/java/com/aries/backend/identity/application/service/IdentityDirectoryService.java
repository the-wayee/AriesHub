package com.aries.backend.identity.application.service;

import com.aries.backend.identity.application.port.UserAvatarStorage;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 对外提供身份只读能力；跨模块适配器不直接访问用户聚合和领域仓储。 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdentityDirectoryService {
    private final UserRepository users;
    private final UserAvatarStorage avatars;

    /** 作者头像与昵称一样按集合补齐，签名由头像端口批量生成。 */
    public Map<Long, String> avatarUrls(Set<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        avatars.publicAvatars(users.findAvatarIdsByIds(ids))
                .forEach((user, image) -> result.put(user, image.url()));
        return result;
    }

    public boolean isAdmin(long userId) {
        return users.findById(userId)
                .map(user -> user.getRole() == UserAccount.Role.ADMIN)
                .orElse(false);
    }

    /** 保持一次批量查询，避免评论列表产生 N+1 查询。 */
    public Map<Long, String> displayNames(Set<Long> userIds) {
        return users.findNicknamesByIds(userIds);
    }
}
