package com.aries.backend.catalog.application.service;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.ADMIN_PUBLICATION_NOT_FOUND;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.CATEGORY_NOT_FOUND;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.PUBLICATION_CONTENT_REQUIRED;

import com.aries.backend.catalog.application.command.SavePublicationCommand;
import com.aries.backend.catalog.application.port.AdminCatalogReadPort;
import com.aries.backend.catalog.application.port.CategoryWritePort;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationSummary;
import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.domain.repository.PublicationRepository;
import com.aries.backend.shared.application.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** 管理员内容用例：编辑始终保存完整快照，发布和下架使用显式动作。 */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private final AdminCatalogReadPort catalog;
    private final CategoryWritePort categories;
    private final PublicationRepository publications;
    private final PublicationMediaService media;

    @Transactional(readOnly = true)
    public List<AdminPublicationSummary> publications() {
        return catalog.publications();
    }

    @Transactional(readOnly = true)
    public AdminPublicationDetail detail(long id) {
        return catalog.find(id)
                .orElseThrow(() -> new BusinessException(ADMIN_PUBLICATION_NOT_FOUND));
    }

    @Transactional
    public AdminPublicationDetail create(SavePublicationCommand command) {
        validateCategory(command.categoryId());
        media.validateReferences(0, command);
        // 公开链接使用数据库唯一 ID；内部旧 slug 列仅为历史兼容，不由标题或客户端生成。
        Publication created =
                publications.save(
                        Publication.create(command.toDraft(UUID.randomUUID().toString())));
        media.bind(created.getId(), command);
        return detail(created.getId());
    }

    @Transactional
    public AdminPublicationDetail update(long id, SavePublicationCommand command) {
        validateCategory(command.categoryId());
        try {
            Publication study = editable(id);
            media.validateReferences(id, command);
            publications.save(study.edit(command.toDraft(study.getSlug())));
            media.bind(id, command);
            return detail(id);
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
        return publications
                .findForEditing(id)
                .orElseThrow(() -> new BusinessException(ADMIN_PUBLICATION_NOT_FOUND));
    }

    private void validateCategory(long categoryId) {
        // 与分类删除串行：持锁直到文章事务提交，防止引用逻辑删除的分类。
        boolean exists = categories.lockExists(categoryId);
        if (!exists) throw new BusinessException(CATEGORY_NOT_FOUND);
    }
}
