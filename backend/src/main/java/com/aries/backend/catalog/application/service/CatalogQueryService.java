package com.aries.backend.catalog.application.service;

import com.aries.backend.catalog.application.port.CatalogReadPort;
import com.aries.backend.catalog.application.query.CaseSearchQuery;
import com.aries.backend.catalog.domain.model.CaseStudy;
import com.aries.backend.catalog.domain.repository.CaseRepository;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.aries.backend.catalog.application.view.CatalogViews.*;
import static com.aries.backend.shared.application.exception.BusinessException.Code.*;

/**
 * 案例浏览用例：编排仓储与领域规则，不处理 HTTP，也不拼接 SQL。
 * 可重复读确保列表数量、详情及访问规则在同一次查询用例中使用一致快照。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CatalogQueryService {
    private final CaseRepository cases;
    private final CatalogReadPort reads;

    public List<Category> categories() { return reads.categories(); }

    public Page<CaseSummary> list(CaseSearchQuery query) {
        long total = reads.count(query);
        return new Page<>(reads.list(query), query.getPage(), query.getSize(), total,
                (total + query.getSize() - 1) / query.getSize());
    }

    public CaseDetail detail(String slug) {
        CaseStudy study = visible(cases.findBySlug(slug).orElseThrow(this::missing));
        CaseSummary summary = reads.findPublicSummary(study.getId());
        Preview preview = reads.preview(study.getId());
        if (summary == null || preview == null) throw missing();
        return new CaseDetail(summary, preview);
    }

    public Content content(long id) {
        CaseStudy study = visible(cases.findById(id).orElseThrow(this::missing));
        // 先执行领域规则；未接入付费权益前，对付费内容始终拒绝访问。
        if (!study.allowsPublicReading()) throw new BusinessException(CONTENT_LOCKED);
        // SQL 再次限制发布状态和免费类型，避免未来调用者绕过应用规则造成泄露。
        Content content = reads.freeContent(id);
        if (content == null) throw missing();
        return content;
    }

    private CaseStudy visible(CaseStudy study) {
        if (!study.isPubliclyVisible()) throw missing();
        return study;
    }
    private BusinessException missing() { return new BusinessException(CASE_NOT_FOUND); }
}
