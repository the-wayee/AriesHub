package com.aries.backend.discussion.interfaces.rest;

import com.aries.backend.shared.interfaces.rest.Result;
import com.aries.backend.discussion.application.service.DiscussionService;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.discussion.interfaces.rest.request.LockThreadRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员评论治理接口。
 *
 * <p>权限由 catalog 声明的 {@code AdminAuthorizationConfiguration} 统一施加：它按
 * {@code /api/v1/admin/**} 这个 URL 模式注册拦截器，与声明它的模块无关，
 * 所以这里的端点自动要求 ADMIN 角色，不需要本模块重复声明权限配置，
 * 服务层也因此不必自己检查角色（角色判断仍由身份端口提供，用于「作者本人或管理员」的删除判断）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/discussions")
public class AdminDiscussionController {
    private final DiscussionService discussions;

    /** 隐藏一条违规评论；与作者自删区分，正文不再对外返回。 */
    @PostMapping("/comments/{id}/hide")
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> hide(@PathVariable @Positive long id) {
        discussions.hide(id);
        return Result.success(null);
    }

    /** 锁定讨论：线程保留可见，但不再接受新的评论和回复。 */
    @PostMapping("/threads/lock")
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> lock(@Valid @RequestBody LockThreadRequest request) {
        discussions.lockThread(new DiscussionTarget(request.targetType(), request.targetKey()));
        return Result.success(null);
    }
}
