package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.application.port.AdminCatalogReadPort;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminCaseDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminCaseSummary;
import com.aries.backend.catalog.infrastructure.persistence.mapper.AdminCatalogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 后台列表与详情的 MyBatis 查询实现，不承担领域写入。 */
@Repository
@RequiredArgsConstructor
public class MybatisAdminCatalogReadRepository implements AdminCatalogReadPort {
    private final AdminCatalogMapper queries;

    @Override
    public List<CategoryOption> categories() {
        return queries.categories();
    }

    @Override
    public List<AdminCaseSummary> cases() {
        return queries.cases();
    }

    @Override
    public Optional<AdminCaseDetail> find(long id) {
        return Optional.ofNullable(queries.find(id));
    }
}
