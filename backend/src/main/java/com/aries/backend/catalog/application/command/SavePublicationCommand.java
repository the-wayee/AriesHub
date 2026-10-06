package com.aries.backend.catalog.application.command;

import com.aries.backend.catalog.domain.model.Publication;

/** 创建或编辑发布内容时的完整快照。 */
public record SavePublicationCommand(
        long categoryId,
        String title,
        String summary,
        String publicationType,
        String accessType,
        long creditPrice,
        String previewMarkdown,
        String fullMarkdown,
        String requirements,
        String deliverables,
        String version,
        String coverFileId,
        boolean featured) {
    /** 兼容旧数据的内部标识由用例提供，不接受客户端指定或修改文章地址。 */
    public Publication.Draft toDraft(String stableSlug) {
        return new Publication.Draft(
                categoryId,
                stableSlug,
                title,
                summary,
                Publication.PublicationType.valueOf(publicationType),
                Publication.AccessType.valueOf(accessType),
                creditPrice,
                new Publication.Content(
                        previewMarkdown, fullMarkdown, requirements, deliverables, version),
                coverFileId,
                featured);
    }
}
