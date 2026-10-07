package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.application.port.CatalogReadPort;
import com.aries.backend.catalog.application.query.PublicationSearchQuery;
import com.aries.backend.catalog.application.view.CatalogViews.Category;
import com.aries.backend.catalog.application.view.CatalogViews.Content;
import com.aries.backend.catalog.application.view.CatalogViews.Preview;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CatalogReadMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

/** 查询端口适配器，让应用层无需依赖 MyBatis 类型或 SQL 细节。 */
@Repository
@RequiredArgsConstructor
public class MybatisCatalogReadRepository implements CatalogReadPort {
    private final CatalogReadMapper mapper;

    @Override
    public List<Category> categories() {
        return mapper.categories();
    }

    @Override
    public List<PublicationSummary> list(PublicationSearchQuery query) {
        return mapper.list(query);
    }

    @Override
    public long count(PublicationSearchQuery query) {
        return mapper.count(query);
    }

    @Override
    public PublicationSummary findPublicSummary(long id) {
        return mapper.findPublicSummary(id);
    }

    @Override
    public Preview preview(long id) {
        return mapper.preview(id);
    }

    @Override
    public Content authorizedContent(long id) {
        return mapper.authorizedContent(id);
    }

    @Override
    public Content freeContent(long id) {
        return mapper.freeContent(id);
    }
}
