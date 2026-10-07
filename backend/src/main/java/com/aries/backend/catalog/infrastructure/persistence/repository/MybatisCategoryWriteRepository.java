package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.application.exception.CatalogErrorCode;
import com.aries.backend.catalog.application.port.CategoryWritePort;
import com.aries.backend.catalog.application.view.CategoryViews.CategoryOption;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CategoryMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.CategoryPO;
import com.aries.backend.shared.application.exception.BusinessException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;

/** 简单插入使用 MyBatis-Plus，不为分类创建引入手写 SQL。 */
@Repository
@RequiredArgsConstructor
public class MybatisCategoryWriteRepository implements CategoryWritePort {
    private final CategoryMapper categories;

    @Override
    public boolean lockExists(long id) {
        return categories.selectOne(
                        Wrappers.<CategoryPO>lambdaQuery()
                                .eq(CategoryPO::getId, id)
                                .last("FOR UPDATE"))
                != null;
    }

    @Override
    public CategoryOption update(long id, String name, String color) {
        CategoryPO row = categories.selectById(id);
        row.setName(name);
        row.setColor(color);
        try {
            categories.updateById(row);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CatalogErrorCode.CATEGORY_NAME_CONFLICT);
        }
        return new CategoryOption(
                row.getId().toString(),
                row.getSlug(),
                row.getName(),
                row.getColor(),
                row.getSortOrder());
    }

    @Override
    public void delete(long id) {
        categories.deleteById(id);
    }

    @Override
    public void reorder(List<Long> categoryIds) {
        // 按 ID 固定顺序加行锁，串行处理并发拖拽，防止出现混合排序或死锁。
        List<CategoryPO> current =
                categories.selectList(
                        Wrappers.<CategoryPO>lambdaQuery()
                                .orderByAsc(CategoryPO::getId)
                                .last("FOR UPDATE"));
        if (categoryIds.size() != current.size()
                || new HashSet<>(categoryIds).size() != categoryIds.size()
                || !new HashSet<>(categoryIds)
                        .equals(new HashSet<>(current.stream().map(CategoryPO::getId).toList()))) {
            throw new BusinessException(CatalogErrorCode.CATEGORY_ORDER_CHANGED);
        }
        for (int index = 0; index < categoryIds.size(); index++) {
            categories.update(
                    null,
                    Wrappers.<CategoryPO>lambdaUpdate()
                            .eq(CategoryPO::getId, categoryIds.get(index))
                            .set(CategoryPO::getSortOrder, index));
        }
    }

    @Override
    public CategoryOption create(String name, String slug, int sortOrder, String color) {
        CategoryPO row = new CategoryPO();
        row.setName(name);
        row.setColor(color);
        row.setSlug(slug);
        row.setSortOrder(sortOrder);
        try {
            categories.insert(row);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CatalogErrorCode.CATEGORY_NAME_CONFLICT);
        }
        return new CategoryOption(
                row.getId().toString(),
                row.getSlug(),
                row.getName(),
                row.getColor(),
                row.getSortOrder());
    }
}
