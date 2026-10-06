package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.application.port.AdminCatalogReadPort;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationSummary;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.infrastructure.persistence.mapper.AdminCatalogMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CategoryMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.CategoryPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 后台列表与详情的 MyBatis 查询实现，不承担领域写入。 */
@Repository
@RequiredArgsConstructor
public class MybatisAdminCatalogReadRepository implements AdminCatalogReadPort {
    private final AdminCatalogMapper queries;
    private final CategoryMapper categories;

    @Override
    public List<CategoryOption> categories() {
        return categories
                .selectList(
                        Wrappers.<CategoryPO>lambdaQuery()
                                .select(CategoryPO::getId, CategoryPO::getSlug, CategoryPO::getName)
                                .orderByAsc(CategoryPO::getSortOrder, CategoryPO::getId))
                .stream()
                .map(
                        row ->
                                new CategoryOption(
                                        row.getId().toString(), row.getSlug(), row.getName()))
                .toList();
    }

    @Override
    public List<AdminPublicationSummary> publications() {
        return queries.publications();
    }

    @Override
    public Optional<AdminPublicationDetail> find(long id) {
        return Optional.ofNullable(queries.find(id));
    }
}
