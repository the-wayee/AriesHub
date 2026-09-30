package com.aries.backend.discussion.interfaces.rest;

import com.aries.backend.discussion.application.service.DiscussionService;
import com.aries.backend.discussion.application.view.DiscussionViews.CommentPage;
import com.aries.backend.discussion.application.view.DiscussionViews.CommentView;
import com.aries.backend.discussion.application.view.DiscussionViews.ReplyPage;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.discussion.interfaces.rest.request.CommentListRequest;
import com.aries.backend.discussion.interfaces.rest.request.CreateCommentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公开评论接口：读取不需要登录，写入由服务层校验登录态。
 * 管理员专用的隐藏与锁帖放在 {@link AdminDiscussionController}，由 /api/v1/admin/** 拦截器保护。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discussions/comments")
public class DiscussionController {
    /** 目标类型与讨论键的校验规则；与 DiscussionTarget 的构造器保持一致。 */
    static final String TARGET_TYPE_PATTERN = "[A-Z][A-Z0-9_]{1,39}";

    private final DiscussionService discussions;

    @GetMapping
    public CommentPage comments(
            @RequestParam @Pattern(regexp = TARGET_TYPE_PATTERN) String targetType,
            @RequestParam @NotBlank @Size(max = 120) String targetKey,
            @Valid @ModelAttribute CommentListRequest query) {
        return discussions.comments(new DiscussionTarget(targetType, targetKey), query.toQuery());
    }

    /** 展开某条根评论下的全部回复；根评论自身由列表接口给出。 */
    @GetMapping("/{rootId}/replies")
    public ReplyPage replies(@PathVariable @Positive long rootId,
                             @Valid @ModelAttribute CommentListRequest query) {
        return discussions.replies(rootId, query.toQuery());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView add(@Valid @RequestBody CreateCommentRequest request) {
        return discussions.add(new DiscussionTarget(request.targetType(), request.targetKey()),
                request.parentId(), request.body());
    }

    /** 作者删除自己的评论；管理员删除他人的评论走这里也会通过权限校验。 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive long id) {
        discussions.delete(id);
    }

    /** 切换点赞状态，返回切换后是否已点赞。 */
    @PostMapping("/{id}/like")
    public Map<String, Boolean> toggleLike(@PathVariable @Positive long id) {
        return Map.of("liked", discussions.toggleLike(id));
    }
}
