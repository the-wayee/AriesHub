package com.aries.backend.catalog.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.aries.backend.catalog.application.query.CaseSearchQuery;
import static com.aries.backend.catalog.application.view.CatalogViews.*;

/** 联表、聚合及安全正文读取使用显式 SQL，便于审查实际返回字段。 */
@Mapper
public interface CatalogReadMapper {
    List<Category> categories();
    List<CaseSummary> list(@Param("query") CaseSearchQuery query);
    long count(@Param("query") CaseSearchQuery query);
    CaseSummary findPublicSummary(long id);
    Preview preview(long id);
    Content freeContent(long id);
}
