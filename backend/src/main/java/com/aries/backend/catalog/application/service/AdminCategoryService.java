package com.aries.backend.catalog.application.service;

import com.aries.backend.catalog.application.exception.CatalogErrorCode;
import com.aries.backend.catalog.application.port.CategoryReadPort;
import com.aries.backend.catalog.application.port.CategoryWritePort;
import com.aries.backend.catalog.application.view.CategoryViews.CategoryOption;
import com.aries.backend.shared.application.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** 分类用于文章与视频内容的主题归档，和案例、文章、课程等内容形式分开维护。 */
@Service
@RequiredArgsConstructor
public class AdminCategoryService {
    private final CategoryWritePort categories;
    private final CategoryReadPort reads;

    @Transactional
    public CategoryOption update(long id, String name, String color) {
        requireCategory(id);
        return categories.update(id, name.strip(), color);
    }

    /** 与文章归类共用分类行锁，避免检查引用后又被并发文章写入引用。 */
    @Transactional
    public void delete(long id) {
        requireCategory(id);
        if (reads.isInUse(id)) throw new BusinessException(CatalogErrorCode.CATEGORY_IN_USE);
        categories.delete(id);
    }

    private void requireCategory(long id) {
        if (!categories.lockExists(id))
            throw new BusinessException(CatalogErrorCode.ADMIN_CATEGORY_NOT_FOUND);
    }

    @Transactional(readOnly = true)
    public List<CategoryOption> categories() {
        return reads.categories();
    }

    /** 一次事务保存完整顺序，避免拖动中间状态被其他请求读取。 */
    @Transactional
    public List<CategoryOption> reorder(List<Long> categoryIds) {
        categories.reorder(categoryIds);
        return reads.categories();
    }

    @Transactional
    public CategoryOption create(String name, int sortOrder, String color) {
        // 内部筛选标识由服务端生成，不让管理员维护易冲突的路径。
        return categories.create(name.strip(), "category-" + UUID.randomUUID(), sortOrder, color);
    }
}
