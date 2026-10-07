package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.view.CategoryViews.CategoryOption;

import java.util.List;

/** 分类写入边界；唯一性由数据库约束保证，避免并发创建同名分类。 */
public interface CategoryWritePort {
    boolean lockExists(long id);

    CategoryOption update(long id, String name, String color);

    void delete(long id);

    void reorder(List<Long> categoryIds);

    CategoryOption create(String name, String slug, int sortOrder, String color);
}
