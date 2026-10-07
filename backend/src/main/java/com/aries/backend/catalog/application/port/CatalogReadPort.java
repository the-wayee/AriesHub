package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.query.PublicationSearchQuery;
import com.aries.backend.catalog.application.view.CatalogViews.Category;
import com.aries.backend.catalog.application.view.CatalogViews.Content;
import com.aries.backend.catalog.application.view.CatalogViews.Preview;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;

import java.util.List;

/** 列表与详情的查询端口；投影查询不必为了列表展示加载完整聚合。 */
public interface CatalogReadPort {
    List<Category> categories();

    List<PublicationSummary> list(PublicationSearchQuery query);

    long count(PublicationSearchQuery query);

    PublicationSummary findPublicSummary(long id);

    Preview preview(long id);

    Content freeContent(long id);

    Content authorizedContent(long id);
}
