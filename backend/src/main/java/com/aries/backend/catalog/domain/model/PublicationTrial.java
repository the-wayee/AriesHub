package com.aries.backend.catalog.domain.model;

/** 试读边界属于内容规则；标记后的正文和素材永远不进入公开预览。 */
public final class PublicationTrial {
    public static final String BOUNDARY = "<!-- arieshub:paid -->";

    private PublicationTrial() {}

    public static String preview(String accessType, String fullMarkdown, String legacyPreview) {
        int boundary = fullMarkdown.indexOf(BOUNDARY);
        if ("FREE".equals(accessType)) return legacyPreview == null ? "" : legacyPreview;
        // 即使旧客户端传入了独立试读，只要正文有边界也必须以服务端截断结果为准。
        if (boundary >= 0) return fullMarkdown.substring(0, boundary).strip();
        return legacyPreview == null ? "" : legacyPreview;
    }
}
