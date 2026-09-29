package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminCaseDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminCaseSummary;

import java.util.List;
import java.util.Optional;

/** 管理后台只读投影；写入通过领域仓储完成。 */
public interface AdminCatalogReadPort {
    List<CategoryOption> categories();
    List<AdminCaseSummary> cases();
    Optional<AdminCaseDetail> find(long id);
}
