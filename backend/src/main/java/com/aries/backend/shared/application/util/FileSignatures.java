package com.aries.backend.shared.application.util;

import static com.aries.backend.shared.application.util.MediaTypes.*;

import static org.springframework.util.MimeTypeUtils.IMAGE_JPEG_VALUE;
import static org.springframework.util.MimeTypeUtils.IMAGE_PNG_VALUE;

import java.util.Arrays;

/** 文件头识别工具，只判断格式签名；不承担用途、尺寸、归属或完整文件解码。 */
public final class FileSignatures {
    /** WebP 签名最长需要读取十二字节；读取后由调用者回填，避免丢失上传内容。 */
    public static final int HEADER_BYTES = 12;

    private static final byte[] PNG = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    private static final byte[] JPEG = {(byte) 255, (byte) 216, (byte) 255};
    private static final byte[] WEBM = {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3};
    private static final String RIFF_SIGNATURE = "RIFF";
    private static final String WEBP_SIGNATURE = "WEBP";
    private static final String MP4_SIGNATURE = "ftyp";

    private FileSignatures() {}

    public static boolean matchesImage(String type, byte[] header) {
        if (type == null) return false;
        return switch (type) {
            case IMAGE_PNG_VALUE -> startsWith(header, PNG);
            case IMAGE_JPEG_VALUE -> startsWith(header, JPEG);
            case IMAGE_WEBP_VALUE ->
                    matchesAscii(header, 0, RIFF_SIGNATURE)
                            && matchesAscii(header, 8, WEBP_SIGNATURE);
            default -> false;
        };
    }

    public static boolean matchesVideo(String type, byte[] header) {
        if (type == null) return false;
        return switch (type) {
            case VIDEO_MP4_VALUE -> matchesAscii(header, 4, MP4_SIGNATURE);
            case VIDEO_WEBM_VALUE -> startsWith(header, WEBM);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] header, byte[] signature) {
        return header.length >= signature.length
                && Arrays.equals(header, 0, signature.length, signature, 0, signature.length);
    }

    private static boolean matchesAscii(byte[] header, int offset, String signature) {
        if (header.length < offset + signature.length()) return false;
        for (int i = 0; i < signature.length(); i++) {
            if (header[offset + i] != signature.charAt(i)) return false;
        }
        return true;
    }
}
