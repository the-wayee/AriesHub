package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.application.command.SaveCaseCommand;
import com.aries.backend.catalog.application.port.AdminCatalogPort;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseSummary;
import com.aries.backend.catalog.infrastructure.persistence.mapper.AdminCatalogMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CaseContentMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CaseMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.CaseContentPO;
import com.aries.backend.catalog.infrastructure.persistence.po.CasePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/** 内容后台端口的 MyBatis 实现。 */
@Repository
@RequiredArgsConstructor
public class MybatisAdminCatalogRepository implements AdminCatalogPort {
    private final AdminCatalogMapper queries;
    private final CaseMapper cases;
    private final CaseContentMapper contents;

    @Override
    public List<CategoryOption> categories() {
        return queries.categories();
    }

    @Override
    public List<CaseSummary> cases() {
        return queries.cases();
    }

    @Override
    public Optional<CaseDetail> find(long id) {
        return Optional.ofNullable(queries.find(id));
    }

    @Override
    public long create(SaveCaseCommand command) {
        CasePO caseRow = new CasePO();
        apply(caseRow, command);
        caseRow.setCurrency("CNY");
        caseRow.setStatus("DRAFT");
        caseRow.setDeliveryStatus("AVAILABLE");
        caseRow.setIsDemo(false);
        cases.insert(caseRow);

        CaseContentPO contentRow = content(caseRow.getId(), command);
        contents.insert(contentRow);
        return caseRow.getId();
    }

    @Override
    public boolean update(long id, SaveCaseCommand command) {
        CasePO caseRow = cases.selectById(id);
        if (caseRow == null) return false;
        apply(caseRow, command);
        cases.updateById(caseRow);

        CaseContentPO contentRow = content(id, command);
        if (contents.selectById(id) == null) contents.insert(contentRow);
        else contents.updateById(contentRow);
        return true;
    }

    @Override
    public boolean publish(long id) {
        CasePO row = cases.selectById(id);
        if (row == null || contents.selectById(id) == null) return false;
        row.setStatus("PUBLISHED");
        row.setDeliveryStatus("AVAILABLE");
        row.setPublishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return cases.updateById(row) == 1;
    }

    @Override
    public boolean archive(long id) {
        CasePO row = cases.selectById(id);
        if (row == null) return false;
        row.setStatus("ARCHIVED");
        return cases.updateById(row) == 1;
    }

    private void apply(CasePO row, SaveCaseCommand command) {
        row.setCategoryId(command.categoryId());
        row.setSlug(command.slug());
        row.setTitle(command.title());
        row.setSummary(command.summary());
        row.setAccessType(command.accessType());
        row.setPriceMinor(command.priceMinor());
    }

    private CaseContentPO content(long caseId, SaveCaseCommand command) {
        CaseContentPO row = new CaseContentPO();
        row.setCaseId(caseId);
        row.setPreviewMarkdown(command.previewMarkdown());
        row.setFullMarkdown(command.fullMarkdown());
        row.setRequirements(command.requirements());
        row.setDeliverables(command.deliverables());
        row.setVersion(command.version());
        return row;
    }
}
