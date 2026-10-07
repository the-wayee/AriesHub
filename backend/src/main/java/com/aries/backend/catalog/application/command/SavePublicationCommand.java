package com.aries.backend.catalog.application.command;

import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.domain.model.PublicationTrial;

import java.util.List;

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
        String coverFileId,
        boolean featured,
        List<String> attachmentIds) {
    public SavePublicationCommand {
        if (attachmentIds != null) attachmentIds = List.copyOf(attachmentIds);
        // 先生成唯一可信的试读快照，再进行素材校验与绑定，避免付费素材误标为公开。
        previewMarkdown = PublicationTrial.preview(accessType, fullMarkdown, previewMarkdown);
    }

    /** 正文版本由用例生成；文章地址只使用数据库分配的 ID。 */
    public Publication.Draft toDraft(String contentVersion) {
        return new Publication.Draft(
                categoryId,
                title,
                summary,
                Publication.PublicationType.valueOf(publicationType),
                Publication.AccessType.valueOf(accessType),
                creditPrice,
                new Publication.Content(previewMarkdown, fullMarkdown, contentVersion),
                coverFileId,
                featured);
    }
}
