package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 后台联表投影查询，完整正文只能从管理员保护路由返回。 */
@Mapper
public interface AdminCatalogMapper {
    List<CategoryOption> categories();
    List<CaseSummary> cases();
    CaseDetail find(@Param("id") long id);
}
