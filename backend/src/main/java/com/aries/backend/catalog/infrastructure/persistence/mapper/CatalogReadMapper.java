package com.aries.backend.catalog.infrastructure.persistence.mapper;

import static com.aries.backend.catalog.application.view.CatalogViews.*;

import com.aries.backend.catalog.application.query.PublicationSearchQuery;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 联表、聚合及安全正文读取使用显式 SQL，便于审查实际返回字段。 */
@Mapper
public interface CatalogReadMapper {
    List<Category> categories();

    List<PublicationSummary> list(@Param("query") PublicationSearchQuery query);

    long count(@Param("query") PublicationSearchQuery query);

    PublicationSummary findPublicSummary(long id);

    Preview preview(long id);

    Content authorizedContent(@org.apache.ibatis.annotations.Param("id") long id);

    Content freeContent(long id);
}
