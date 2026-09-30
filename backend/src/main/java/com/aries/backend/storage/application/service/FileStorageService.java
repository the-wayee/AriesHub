package com.aries.backend.storage.application.service;

import com.aries.backend.shared.application.exception.BusinessException;
import com.aries.backend.storage.application.port.*;
import com.aries.backend.storage.domain.model.StoredFile;
import com.aries.backend.storage.domain.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static com.aries.backend.storage.application.exception.StorageErrorCode.*;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    public static final long AVATAR_MAX_BYTES = 5 * 1024 * 1024;
    public static final long ATTACHMENT_MAX_BYTES = 20 * 1024 * 1024;
    public static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/webp", "webp",
            "application/pdf", "pdf", "application/zip", "zip");
    private final StorageIdentityProvider identity;
    private final UploadRateLimiter rateLimiter;
    private final ObjectStorage objects;
    private final StoredFileRepository files;

    public StoredFile upload(StoredFile.Purpose purpose, String filename, String contentType,
                             long size, InputStream content) {
        long owner = identity.currentUserId();
        if (purpose == null || contentType == null || !EXTENSIONS.containsKey(contentType)
                || size <= 0 || size > (purpose == StoredFile.Purpose.AVATAR ? AVATAR_MAX_BYTES : ATTACHMENT_MAX_BYTES)
                || (purpose == StoredFile.Purpose.AVATAR && !contentType.startsWith("image/"))) {
            throw new BusinessException(INVALID_FILE);
        }
        rateLimiter.check(owner);
        UUID id = UUID.randomUUID();
        String key = "uploads/" + owner + "/" + purpose.name().toLowerCase(java.util.Locale.ROOT)
                + "/" + id + "." + EXTENSIONS.get(contentType);
        StoredFile file = new StoredFile(id, owner, purpose, key, safeFilename(filename), contentType, size);
        // 校验真实文件头，且将读取的字节放回流中，避免丢失上传内容。
        try {
            PushbackInputStream stream = new PushbackInputStream(content, 12);
            byte[] header = stream.readNBytes(12);
            if (!matches(contentType, header)) throw new BusinessException(INVALID_FILE);
            stream.unread(header);
            objects.put(key, stream, size, contentType);
        } catch (IOException error) {
            throw new BusinessException(INVALID_FILE);
        }
        try {
            files.save(file);
        } catch (RuntimeException error) {
            // 元数据保存失败时清理已经上传的对象；保留原始异常，不以清理错误覆盖它。
            try { objects.delete(key); } catch (RuntimeException cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
        return file;
    }

    public record Download(String url, Instant expiresAt) {}

    public Download download(UUID fileId) {
        long owner = identity.currentUserId();
        StoredFile file = files.findById(fileId).filter(value -> value.ownerId() == owner)
                .orElseThrow(() -> new BusinessException(FILE_NOT_FOUND));
        Instant expiresAt = Instant.now().plus(DOWNLOAD_TTL);
        return new Download(objects.downloadUrl(file.objectKey(), file.filename(), DOWNLOAD_TTL), expiresAt);
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        String clean = filename.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        return clean.isEmpty() ? "file" : clean.substring(0, Math.min(180, clean.length()));
    }

    private boolean matches(String type, byte[] h) {
        return switch (type) {
            case "image/png" -> h.length >= 8 && Arrays.equals(Arrays.copyOf(h, 8),
                    new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10});
            case "image/jpeg" -> h.length >= 3 && h[0] == (byte)255 && h[1] == (byte)216 && h[2] == (byte)255;
            case "image/webp" -> h.length >= 12 && ascii(h, 0, "RIFF") && ascii(h, 8, "WEBP");
            case "application/pdf" -> h.length >= 5 && ascii(h, 0, "%PDF-");
            case "application/zip" -> h.length >= 4 && h[0] == 80 && h[1] == 75
                    && ((h[2] == 3 && h[3] == 4) || (h[2] == 5 && h[3] == 6));
            default -> false;
        };
    }

    private boolean ascii(byte[] header, int offset, String value) {
        for (int i = 0; i < value.length(); i++) if (header[offset + i] != value.charAt(i)) return false;
        return true;
    }
}
