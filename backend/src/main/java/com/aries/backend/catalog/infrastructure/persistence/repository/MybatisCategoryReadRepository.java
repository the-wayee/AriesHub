package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.application.port.CategoryReadPort;
import com.aries.backend.catalog.application.view.CategoryViews.CategoryOption;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CategoryMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.CategoryPO;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

/** 分类简单查询通过 MyBatis-Plus 完成，保持稳定的排序规则。 */
@Repository
@RequiredArgsConstructor
public class MybatisCategoryReadRepository implements CategoryReadPort {
    private final CategoryMapper categories;
    private final PublicationMapper publications;

    @Override
    public boolean isInUse(long id) {
        // 草稿和已归档内容也属于有效引用，不能因删除分类而消失。
        return publications.selectCount(
                        Wrappers.<PublicationPO>lambdaQuery().eq(PublicationPO::getCategoryId, id))
                > 0;
    }

    @Override
    public List<CategoryOption> categories() {
        return categories
                .selectList(
                        Wrappers.<CategoryPO>lambdaQuery()
                                .select(
                                        CategoryPO::getId,
                                        CategoryPO::getSlug,
                                        CategoryPO::getName,
                                        CategoryPO::getColor,
                                        CategoryPO::getSortOrder)
                                .orderByAsc(CategoryPO::getSortOrder, CategoryPO::getId))
                .stream()
                .map(
                        row ->
                                new CategoryOption(
                                        row.getId().toString(),
                                        row.getSlug(),
                                        row.getName(),
                                        row.getColor(),
                                        row.getSortOrder()))
                .toList();
    }
}
