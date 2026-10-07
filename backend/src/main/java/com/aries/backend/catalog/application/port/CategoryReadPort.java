package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.view.CategoryViews.CategoryOption;

import java.util.List;

/** 分类选项查询边界，供分类管理和文章归类校验共用。 */
public interface CategoryReadPort {
    List<CategoryOption> categories();

    boolean isInUse(long id);
}
