package com.aries.backend.catalog.application.service;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.CONTENT_LOCKED;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.PUBLICATION_NOT_FOUND;

import com.aries.backend.catalog.application.port.CatalogReadPort;
import com.aries.backend.catalog.application.query.PublicationSearchQuery;
import com.aries.backend.catalog.application.view.CatalogViews.Category;
import com.aries.backend.catalog.application.view.CatalogViews.Content;
import com.aries.backend.catalog.application.view.CatalogViews.Page;
import com.aries.backend.catalog.application.view.CatalogViews.Preview;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationDetail;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;
import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.domain.repository.PublicationRepository;
import com.aries.backend.shared.application.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 发布内容浏览用例：编排仓储与领域规则，不处理 HTTP，也不拼接 SQL。 可重复读确保列表数量、详情及访问规则在同一次查询用例中使用一致快照。 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CatalogQueryService {
    private final PublicationRepository publications;
    private final CatalogReadPort reads;

    /** 跨模块查询可见性时只暴露结果，领域聚合和仓储留在 catalog 内。 */
    public boolean isPubliclyVisibleBySlug(String slug) {
        return publications.findBySlug(slug).filter(Publication::isPubliclyVisible).isPresent();
    }

    public List<Category> categories() {
        return reads.categories();
    }

    public Page<PublicationSummary> list(PublicationSearchQuery query) {
        long total = reads.count(query);
        return new Page<>(
                reads.list(query),
                query.getPage(),
                query.getSize(),
                total,
                (total + query.getSize() - 1) / query.getSize());
    }

    /** ID 为规范地址；旧 slug 只保留读取兼容，便于已有链接重定向。 */
    public PublicationDetail detail(String reference) {
        if (reference.matches("[0-9]+")) {
            try {
                return detail(Long.parseLong(reference));
            } catch (NumberFormatException invalidId) {
                throw missing();
            }
        }
        return detailOf(publications.findBySlug(reference).orElseThrow(this::missing));
    }

    public PublicationDetail detail(long id) {
        return detailOf(publications.findById(id).orElseThrow(this::missing));
    }

    private PublicationDetail detailOf(Publication source) {
        Publication publication = visible(source);
        PublicationSummary summary = reads.findPublicSummary(publication.getId());
        Preview preview = reads.preview(publication.getId());
        if (summary == null || preview == null) throw missing();
        // 普通可见性查询不加载正文；验证发布状态后才读取内容，并只投影目录标题。
        Publication withContent =
                publications.findForEditing(publication.getId()).orElseThrow(this::missing);
        return new PublicationDetail(summary, preview, PublicationOutline.chapters(withContent));
    }

    public Content content(long id) {
        Publication publication = visible(publications.findById(id).orElseThrow(this::missing));
        // 先执行领域规则；未接入付费权益前，对付费内容始终拒绝访问。
        if (!publication.allowsPublicReading()) throw new BusinessException(CONTENT_LOCKED);
        // SQL 再次限制发布状态和免费类型，避免未来调用者绕过应用规则造成泄露。
        Content content = reads.freeContent(id);
        if (content == null) throw missing();
        return content;
    }

    private Publication visible(Publication publication) {
        if (!publication.isPubliclyVisible()) throw missing();
        return publication;
    }

    private BusinessException missing() {
        return new BusinessException(PUBLICATION_NOT_FOUND);
    }
}
