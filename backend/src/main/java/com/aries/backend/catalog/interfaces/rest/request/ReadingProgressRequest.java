package com.aries.backend.catalog.interfaces.rest.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 百分比只表示实际免费正文阅读位置，版本必须匹配服务器当前正文。 */
public record ReadingProgressRequest(
        @NotBlank @Size(max = 32) String version,
        @NotNull @Pattern(regexp = "[a-z0-9-]{0,120}") String position,
        @NotNull @Min(0) @Max(100) Integer percent) {}
