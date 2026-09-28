package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.query.CaseSearchQuery;
import java.util.List;
import static com.aries.backend.catalog.application.view.CatalogViews.*;

/** 列表与详情的查询端口；投影查询不必为了列表展示加载完整聚合。 */
public interface CatalogReadPort {
    List<Category> categories();
    List<CaseSummary> list(CaseSearchQuery query);
    long count(CaseSearchQuery query);
    CaseSummary findPublicSummary(long id);
    Preview preview(long id);
    Content freeContent(long id);
}
