package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationSummary;

import java.util.List;
import java.util.Optional;

/** 管理后台只读投影；写入通过领域仓储完成。 */
public interface AdminCatalogReadPort {
    List<AdminPublicationSummary> publications();

    Optional<AdminPublicationDetail> find(long id);
}
