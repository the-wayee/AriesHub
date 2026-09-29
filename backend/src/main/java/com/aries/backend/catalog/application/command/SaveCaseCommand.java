package com.aries.backend.catalog.application.command;

import com.aries.backend.catalog.domain.model.CaseStudy;

/** 创建或编辑案例时的完整内容快照。 */
public record SaveCaseCommand(
        long categoryId,
        String slug,
        String title,
        String summary,
        String accessType,
        long priceMinor,
        String previewMarkdown,
        String fullMarkdown,
        String requirements,
        String deliverables,
        String version
) {
    public CaseStudy.Draft toDraft() {
        return new CaseStudy.Draft(categoryId, slug, title, summary,
                CaseStudy.AccessType.valueOf(accessType), priceMinor,
                new CaseStudy.Content(previewMarkdown, fullMarkdown, requirements, deliverables, version));
    }
}
