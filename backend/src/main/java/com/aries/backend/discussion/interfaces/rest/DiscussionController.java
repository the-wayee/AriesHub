package com.aries.backend.discussion.interfaces.rest;

import com.aries.backend.discussion.application.service.DiscussionService;
import com.aries.backend.discussion.application.view.CommentNode;
import com.aries.backend.discussion.domain.model.DiscussionTarget;
import com.aries.backend.discussion.interfaces.rest.request.CreateCommentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discussions/comments")
public class DiscussionController {
    private final DiscussionService discussions;

    @GetMapping
    public List<CommentNode> comments(
            @RequestParam @Pattern(regexp = "[A-Z][A-Z0-9_]{1,39}") String targetType,
            @RequestParam @NotBlank @Size(max = 120) String targetKey) {
        return discussions.comments(new DiscussionTarget(targetType, targetKey));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentNode add(@Valid @RequestBody CreateCommentRequest request) {
        return discussions.add(new DiscussionTarget(request.targetType(), request.targetKey()),
                request.parentId(), request.body());
    }
}
