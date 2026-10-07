package com.aries.backend.catalog.application.service;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.ADMIN_PUBLICATION_NOT_FOUND;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.CATEGORY_NOT_FOUND;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.PUBLICATION_CONTENT_REQUIRED;

import com.aries.backend.catalog.application.command.SavePublicationCommand;
import com.aries.backend.catalog.application.port.AdminCatalogReadPort;
import com.aries.backend.catalog.application.port.CategoryWritePort;
import com.aries.backend.catalog.application.port.PublicationCoverPort;
import com.aries.backend.catalog.application.port.PublicationMediaPort.SignedUrl;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationDetailRow;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationListItem;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 管理员内容用例：编辑始终保存完整快照，发布和下架使用显式动作。 */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private final AdminCatalogReadPort catalog;
    private final PublicationCoverPort covers;
    private final CategoryWritePort categories;
    private final PublicationRepository publications;
    private final PublicationMediaService media;

    @Transactional(readOnly = true)
    public List<AdminPublicationListItem> publications() {
        List<AdminPublicationSummary> items = catalog.publications();
        // ADMIN 守卫已验证权限；复用批量元数据读取，避免逐张封面的额外 HTTP 请求。
        Map<String, SignedUrl> urls =
                covers.sign(
                        items.stream()
                                .map(AdminPublicationSummary::coverFileId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList());
        return items.stream()
                .map(item -> new AdminPublicationListItem(item, urls.get(item.coverFileId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPublicationDetail detail(long id) {
        AdminPublicationDetailRow row =
                catalog.find(id)
                        .orElseThrow(() -> new BusinessException(ADMIN_PUBLICATION_NOT_FOUND));
        SignedUrl cover =
                row.coverFileId() == null
                        ? null
                        : covers.sign(List.of(row.coverFileId())).get(row.coverFileId());
        return new AdminPublicationDetail(row, cover);
    }

    @Transactional
    public AdminPublicationDetail create(SavePublicationCommand command) {
        validateCategory(command.categoryId());
        media.validateReferences(0, command);
        // 公开链接使用数据库唯一 ID；内部旧 slug 列仅为历史兼容，不由标题或客户端生成。
        Publication created =
                publications.save(
                        Publication.create(
                                command.toDraft(
                                        UUID.randomUUID().toString(), newContentVersion())));
        media.bind(created.getId(), command);
        return detail(created.getId());
    }

    @Transactional
    public AdminPublicationDetail update(long id, SavePublicationCommand command) {
        validateCategory(command.categoryId());
        try {
            Publication study = editable(id);
            media.validateReferences(id, command);
            // 编辑锁内比较正文，只有正文改变才使旧阅读位置失效；元数据调整保留阅读进度。
            String contentVersion =
                    Objects.equals(study.getContent().fullMarkdown(), command.fullMarkdown())
                            ? study.getContent().version()
                            : newContentVersion();
            publications.save(study.edit(command.toDraft(study.getSlug(), contentVersion)));
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

    private static String newContentVersion() {
        return UUID.randomUUID().toString().replace("-", "");
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
