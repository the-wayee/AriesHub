package com.aries.backend.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 按显示顺序提交完整分类 ID 列表；服务端校验遗漏及重复，拒绝过期列表。 */
public record ReorderCategoriesRequest(
        @NotEmpty @Size(max = 1000) List<@NotNull @Positive Long> categoryIds) {}
