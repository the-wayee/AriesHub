package com.aries.backend.shared.application.util;

/** Spring MimeTypeUtils 未提供的标准 MIME 值；已有值直接使用 Spring 常量，不重复声明。 */
public final class MediaTypes {
    public static final String IMAGE_WEBP_VALUE = "image/webp";
    public static final String VIDEO_MP4_VALUE = "video/mp4";
    public static final String VIDEO_WEBM_VALUE = "video/webm";

    private MediaTypes() {}
}
