package com.aries.backend.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** 更新名称与颜色，稳定标识和排序分别由创建与排序接口维护。 */
public record UpdateCategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color) {
    public UpdateCategoryRequest {
        name = name == null ? null : name.strip();
        color = color == null ? null : color.toUpperCase(Locale.ROOT);
    }
}
