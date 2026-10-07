package com.aries.backend.catalog.application.view;

/** 分类的只读返回模型，不混入文章后台编辑视图。 */
public final class CategoryViews {
    private CategoryViews() {}

    public record CategoryOption(
            String id, String slug, String name, String color, int sortOrder) {}
}
