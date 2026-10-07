package com.aries.backend.identity.application.service;

import static com.aries.backend.identity.application.exception.IdentityErrorCode.*;

import com.aries.backend.identity.application.port.AdminUserPort;
import com.aries.backend.identity.application.port.AdminUserPort.User;
import com.aries.backend.identity.application.port.SessionManager;
import com.aries.backend.identity.domain.model.UserAccount;
import com.aries.backend.shared.application.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 后台成员用例：管理员账号受保护，停用普通成员时撤销已有会话。 */
@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final AdminUserPort users;
    private final SessionManager sessions;

    public record Page(List<AdminUserPort.User> items, int page, int size, long total) {}

    @Transactional(readOnly = true)
    public Page list(String q, String status, int page, int size) {
        String query = q.trim();
        return new Page(
                users.list(query, status, size, (page - 1) * size),
                page,
                size,
                users.count(query, status));
    }

    /** 状态落库后注销成员的全部登录态；管理员不可通过该入口被停用。 */
    @Transactional
    public AdminUserPort.User changeStatus(long id, String status) {
        User current = users.find(id);
        if (current == null) throw new BusinessException(ADMIN_USER_NOT_FOUND);
        if (UserAccount.Role.ADMIN.name().equals(current.role()))
            throw new BusinessException(ADMIN_USER_PROTECTED);
        if (!users.changeMemberStatus(id, status))
            throw new BusinessException(ADMIN_USER_NOT_FOUND);
        if (UserAccount.Status.DISABLED.name().equals(status)) sessions.revoke(id);
        return users.find(id);
    }
}
