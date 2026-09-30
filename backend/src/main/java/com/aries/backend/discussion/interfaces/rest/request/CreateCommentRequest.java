package com.aries.backend.discussion.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 发表评论或回复。
 *
 * <p>{@code parentId} 是<b>根评论</b>的 id：回复结构是单层的，回复某条回复时仍然挂在同一个根评论下，
 * 界面据服务端返回的 {@code parentId} 显示「回复 @某人」。
 */
public record CreateCommentRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,39}") String targetType,
        @NotBlank @Size(max = 120) String targetKey,
        @Positive Long parentId,
        @NotBlank @Size(max = 4000) String body) {}
