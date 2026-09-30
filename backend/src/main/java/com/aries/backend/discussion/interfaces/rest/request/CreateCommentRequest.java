package com.aries.backend.discussion.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,39}") String targetType,
        @NotBlank @Size(max = 120) String targetKey,
        @Positive Long parentId,
        @NotBlank @Size(max = 4000) String body) {}
