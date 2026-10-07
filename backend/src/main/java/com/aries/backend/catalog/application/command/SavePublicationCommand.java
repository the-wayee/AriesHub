package com.aries.backend.catalog.application.command;

import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.domain.model.PublicationTrial;

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
        String coverFileId,
        boolean featured) {
    public SavePublicationCommand {
        // 先生成唯一可信的试读快照，再进行素材校验与绑定，避免付费素材误标为公开。
        previewMarkdown = PublicationTrial.preview(accessType, fullMarkdown, previewMarkdown);
    }

    /** 兼容旧数据的内部标识由用例提供，不接受客户端指定或修改文章地址。 */
    public Publication.Draft toDraft(String stableSlug, String contentVersion) {
        return new Publication.Draft(
                categoryId,
                stableSlug,
                title,
                summary,
                Publication.PublicationType.valueOf(publicationType),
                Publication.AccessType.valueOf(accessType),
                creditPrice,
                new Publication.Content(
                        previewMarkdown, fullMarkdown, requirements, deliverables, contentVersion),
                coverFileId,
                featured);
    }
}
