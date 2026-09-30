package com.aries.backend.catalog.application.command;

import com.aries.backend.catalog.domain.model.Publication;

/** 创建或编辑发布内容时的完整快照。 */
public record SavePublicationCommand(
        long categoryId,
        String slug,
        String title,
        String summary,
        String publicationType,
        String accessType,
        long creditPrice,
        String previewMarkdown,
        String fullMarkdown,
        String requirements,
        String deliverables,
        String version
) {
    public Publication.Draft toDraft() {
        return new Publication.Draft(categoryId, slug, title, summary,
                Publication.PublicationType.valueOf(publicationType),
                Publication.AccessType.valueOf(accessType), creditPrice,
                new Publication.Content(previewMarkdown, fullMarkdown, requirements, deliverables, version));
    }
}
