package com.aries.backend.catalog.application.service;

import com.aries.backend.catalog.application.command.SaveCaseCommand;
import com.aries.backend.catalog.application.port.AdminCatalogPort;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseSummary;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.aries.backend.shared.application.exception.BusinessException.Code.*;

/** 管理员内容用例：编辑始终保存完整快照，发布和下架使用显式动作。 */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private final AdminCatalogPort catalog;

    @Transactional(readOnly = true)
    public List<CategoryOption> categories() {
        return catalog.categories();
    }

    @Transactional(readOnly = true)
    public List<CaseSummary> cases() {
        return catalog.cases();
    }

    @Transactional(readOnly = true)
    public CaseDetail detail(long id) {
        return catalog.find(id).orElseThrow(() -> new BusinessException(ADMIN_CASE_NOT_FOUND));
    }

    @Transactional
    public CaseDetail create(SaveCaseCommand command) {
        validateCategory(command.categoryId());
        try {
            return detail(catalog.create(command));
        } catch (DuplicateKeyException error) {
            throw new BusinessException(CASE_SLUG_CONFLICT);
        }
    }

    @Transactional
    public CaseDetail update(long id, SaveCaseCommand command) {
        validateCategory(command.categoryId());
        try {
            if (!catalog.update(id, command)) throw new BusinessException(ADMIN_CASE_NOT_FOUND);
            return detail(id);
        } catch (DuplicateKeyException error) {
            throw new BusinessException(CASE_SLUG_CONFLICT);
        }
    }

    @Transactional
    public CaseDetail publish(long id) {
        if (!catalog.publish(id)) throw new BusinessException(ADMIN_CASE_NOT_FOUND);
        return detail(id);
    }

    @Transactional
    public CaseDetail archive(long id) {
        if (!catalog.archive(id)) throw new BusinessException(ADMIN_CASE_NOT_FOUND);
        return detail(id);
    }

    private void validateCategory(long categoryId) {
        boolean exists = catalog.categories().stream()
                .anyMatch(category -> Long.parseLong(category.id()) == categoryId);
        if (!exists) throw new BusinessException(CATEGORY_NOT_FOUND);
    }
}
