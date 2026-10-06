package com.aries.backend.shared.application.util;

import static com.aries.backend.shared.application.util.MediaTypes.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.MimeTypeUtils.IMAGE_JPEG_VALUE;
import static org.springframework.util.MimeTypeUtils.IMAGE_PNG_VALUE;

import org.junit.jupiter.api.Test;

/** 共用识别工具不能接受伪造声明或截断签名；业务尺寸和授权仍由各领域测试。 */
class FileSignaturesTest {
    @Test
    void declaredImageTypeMustMatchItsSignature() {
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        assertThat(FileSignatures.matchesImage(IMAGE_PNG_VALUE, png)).isTrue();
        assertThat(FileSignatures.matchesImage(IMAGE_JPEG_VALUE, png)).isFalse();
        assertThat(FileSignatures.matchesImage(IMAGE_PNG_VALUE, new byte[] {(byte) 137, 80}))
                .isFalse();
        assertThat(
                        FileSignatures.matchesImage(
                                IMAGE_WEBP_VALUE,
                                "RIFF0000WEBP"
                                        .getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                .isTrue();
        assertThat(
                        FileSignatures.matchesImage(
                                IMAGE_WEBP_VALUE,
                                "RIFF0000".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                .isFalse();
    }

    @Test
    void videoAndImageDeclarationsCannotBeInterchanged() {
        byte[] mp4 = {0, 0, 0, 12, 'f', 't', 'y', 'p', 0, 0, 0, 0};
        byte[] webm = {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3};
        assertThat(FileSignatures.matchesVideo(VIDEO_MP4_VALUE, mp4)).isTrue();
        assertThat(FileSignatures.matchesImage(IMAGE_PNG_VALUE, mp4)).isFalse();
        assertThat(FileSignatures.matchesVideo(VIDEO_WEBM_VALUE, webm)).isTrue();
        assertThat(FileSignatures.matchesVideo(VIDEO_MP4_VALUE, webm)).isFalse();
        assertThat(FileSignatures.matchesVideo(VIDEO_MP4_VALUE, new byte[] {0, 0})).isFalse();
    }
}
