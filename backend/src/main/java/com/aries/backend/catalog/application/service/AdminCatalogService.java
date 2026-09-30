package com.aries.backend.catalog.application.service;

import com.aries.backend.catalog.application.command.SavePublicationCommand;
import com.aries.backend.catalog.application.port.AdminCatalogReadPort;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationSummary;
import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.domain.repository.PublicationRepository;
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
    private final PublicationRepository publications;

    @Transactional(readOnly = true)
    public List<CategoryOption> categories() {
        return catalog.categories();
    }

    @Transactional(readOnly = true)
    public List<AdminPublicationSummary> publications() {
        return catalog.publications();
    }

    @Transactional(readOnly = true)
    public AdminPublicationDetail detail(long id) {
        return catalog.find(id).orElseThrow(() -> new BusinessException(ADMIN_PUBLICATION_NOT_FOUND));
    }

    @Transactional
    public AdminPublicationDetail create(SavePublicationCommand command) {
        validateCategory(command.categoryId());
        try {
            Publication created = publications.save(Publication.create(command.toDraft()));
            return detail(created.getId());
        } catch (DuplicateKeyException error) {
            throw new BusinessException(PUBLICATION_SLUG_CONFLICT);
        }
    }

    @Transactional
    public AdminPublicationDetail update(long id, SavePublicationCommand command) {
        validateCategory(command.categoryId());
        try {
            Publication study = editable(id);
            publications.save(study.edit(command.toDraft()));
            return detail(id);
        } catch (DuplicateKeyException error) {
            throw new BusinessException(PUBLICATION_SLUG_CONFLICT);
        } catch (Publication.MissingContent error) {
            throw new BusinessException(PUBLICATION_CONTENT_REQUIRED);
        }
    }

    @Transactional
    public AdminPublicationDetail publish(long id) {
        try {
            publications.save(editable(id).publish(OffsetDateTime.now(ZoneOffset.UTC)));
        } catch (Publication.MissingContent error) {
            throw new BusinessException(PUBLICATION_CONTENT_REQUIRED);
        }
        return detail(id);
    }

    @Transactional
    public AdminPublicationDetail archive(long id) {
        publications.save(editable(id).archive());
        return detail(id);
    }

    private Publication editable(long id) {
        return publications.findForEditing(id)
                .orElseThrow(() -> new BusinessException(ADMIN_PUBLICATION_NOT_FOUND));
    }

    private void validateCategory(long categoryId) {
        boolean exists = catalog.categories().stream()
                .anyMatch(category -> Long.parseLong(category.id()) == categoryId);
        if (!exists) throw new BusinessException(CATEGORY_NOT_FOUND);
    }
}
