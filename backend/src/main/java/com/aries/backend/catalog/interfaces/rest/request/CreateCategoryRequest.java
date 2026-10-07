package com.aries.backend.catalog.interfaces.rest.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** 分类名称必填，排序越小越靠前；客户端不能指定分类 ID 和内部标识。 */
public record CreateCategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Min(0) @Max(10000) Integer sortOrder,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color) {
    public static final String DEFAULT_COLOR = "#4967A9";

    public CreateCategoryRequest {
        // Jackson 对缺失的原始类型参数会拒绝绑定；显式提供契约约定的默认排序。
        sortOrder = sortOrder == null ? 0 : sortOrder;
        color = color == null ? DEFAULT_COLOR : color.toUpperCase(Locale.ROOT);
        name = name == null ? null : name.strip();
    }
}
