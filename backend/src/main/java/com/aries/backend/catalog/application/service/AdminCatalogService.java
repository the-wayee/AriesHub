package com.aries.backend.catalog.application.service;

import com.aries.backend.catalog.application.command.SaveCaseCommand;
import com.aries.backend.catalog.application.port.AdminCatalogReadPort;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminCaseDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminCaseSummary;
import com.aries.backend.catalog.domain.model.CaseStudy;
import com.aries.backend.catalog.domain.repository.CaseRepository;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.*;

/** 管理员内容用例：编辑始终保存完整快照，发布和下架使用显式动作。 */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private final AdminCatalogReadPort catalog;
    private final CaseRepository cases;

    @Transactional(readOnly = true)
    public List<CategoryOption> categories() {
        return catalog.categories();
    }

    @Transactional(readOnly = true)
    public List<AdminCaseSummary> cases() {
        return catalog.cases();
    }

    @Transactional(readOnly = true)
    public AdminCaseDetail detail(long id) {
        return catalog.find(id).orElseThrow(() -> new BusinessException(ADMIN_CASE_NOT_FOUND));
    }

    @Transactional
    public AdminCaseDetail create(SaveCaseCommand command) {
        validateCategory(command.categoryId());
        try {
            CaseStudy created = cases.save(CaseStudy.create(command.toDraft()));
            return detail(created.getId());
        } catch (DuplicateKeyException error) {
            throw new BusinessException(CASE_SLUG_CONFLICT);
        }
    }

    @Transactional
    public AdminCaseDetail update(long id, SaveCaseCommand command) {
        validateCategory(command.categoryId());
        try {
            CaseStudy study = editable(id);
            cases.save(study.edit(command.toDraft()));
            return detail(id);
        } catch (DuplicateKeyException error) {
            throw new BusinessException(CASE_SLUG_CONFLICT);
        } catch (CaseStudy.MissingContent error) {
            throw new BusinessException(CASE_CONTENT_REQUIRED);
        }
    }

    @Transactional
    public AdminCaseDetail publish(long id) {
        try {
            cases.save(editable(id).publish(OffsetDateTime.now(ZoneOffset.UTC)));
        } catch (CaseStudy.MissingContent error) {
            throw new BusinessException(CASE_CONTENT_REQUIRED);
        }
        return detail(id);
    }

    @Transactional
    public AdminCaseDetail archive(long id) {
        cases.save(editable(id).archive());
        return detail(id);
    }

    private CaseStudy editable(long id) {
        return cases.findForEditing(id)
                .orElseThrow(() -> new BusinessException(ADMIN_CASE_NOT_FOUND));
    }

    private void validateCategory(long categoryId) {
        boolean exists = catalog.categories().stream()
                .anyMatch(category -> Long.parseLong(category.id()) == categoryId);
        if (!exists) throw new BusinessException(CATEGORY_NOT_FOUND);
    }
}
