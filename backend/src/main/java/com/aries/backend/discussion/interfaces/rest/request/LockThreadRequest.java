package com.aries.backend.discussion.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 锁定讨论的入参：按挂载目标定位线程，与评论接口使用同一套类型与键规则。 */
public record LockThreadRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,39}") String targetType,
        @NotBlank @Size(max = 120) String targetKey) {}
