package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.query.PublicationSearchQuery;
import com.aries.backend.catalog.application.view.CatalogViews.Category;
import com.aries.backend.catalog.application.view.CatalogViews.Content;
import com.aries.backend.catalog.application.view.CatalogViews.Preview;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;

import java.util.List;
import java.util.Set;

/** 列表与详情的查询端口；投影查询不必为了列表展示加载完整聚合。 */
public interface CatalogReadPort {
    /** 按文章 ID 批量读取公开摘要；空集合返回空列表，不可见或不存在的 ID 被省略。 */
    List<PublicationSummary> publicSummaries(Set<Long> ids);

    /** 返回未删除分类及其公开文章数，按后台排序展示。 */
    List<Category> categories();

    /** 按筛选、排序和分页返回公开摘要，禁止加载付费正文。 */
    List<PublicationSummary> list(PublicationSearchQuery query);

    /** 与 list 使用同一筛选条件，供分页计算总数。 */
    long count(PublicationSearchQuery query);

    /** 按文章 ID 查询公开摘要；不存在、分类删除或文章下架时返回 null。 */
    PublicationSummary findPublicSummary(long id);

    /** 返回当前公开试读和正文版本；不可见文章返回 null。 */
    Preview preview(long id);

    /** 仅读取公开免费正文；分享用例验证令牌后才能调用，不授予全站权限。 */
    Content freeContent(long id);

    /** 调用方须先验证登录、可见性及付费权益；不得直接用于匿名 HTTP 接口。 */
    Content authorizedContent(long id);
}
