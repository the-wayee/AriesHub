package com.aries.backend.catalog.application.command;

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
) {}
