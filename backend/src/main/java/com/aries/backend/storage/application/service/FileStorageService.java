package com.aries.backend.storage.application.service;

import static com.aries.backend.storage.application.exception.StorageErrorCode.*;

import com.aries.backend.shared.application.exception.BusinessException;
import com.aries.backend.storage.application.port.ObjectStorage;
import com.aries.backend.storage.domain.model.StoredFile;
import com.aries.backend.storage.domain.repository.StoredFileRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** 通用文件读写工具；调用业务负责认证、用途、格式、大小及授权规则。 */
@Service
@RequiredArgsConstructor
public class FileStorageService {
    public static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);
    private final ObjectStorage objects;
    private final StoredFileRepository files;

    public StoredFile upload(
            long owner,
            StoredFile.Purpose purpose,
            String filename,
            String contentType,
            long size,
            InputStream content) {
        return upload(owner, purpose, filename, contentType, size, content, null);
    }

    /** 通用存储进度回调；上传任务、界面阶段和权限仍由调用业务管理。 */
    public StoredFile upload(
            long owner,
            StoredFile.Purpose purpose,
            String filename,
            String contentType,
            long size,
            InputStream content,
            java.util.function.LongConsumer confirmedBytes) {
        if (purpose == null || contentType == null || contentType.isBlank() || size <= 0)
            throw new BusinessException(INVALID_FILE);
        UUID id = UUID.randomUUID();
        String key =
                "uploads/"
                        + owner
                        + "/"
                        + purpose.name().toLowerCase(java.util.Locale.ROOT)
                        + "/"
                        + id;
        StoredFile file =
                new StoredFile(id, owner, purpose, key, safeFilename(filename), contentType, size);
        if (confirmedBytes == null) objects.put(key, content, size, contentType);
        else objects.put(key, content, size, contentType, confirmedBytes);
        try {
            files.save(file);
        } catch (RuntimeException error) {
            try {
                objects.delete(key);
            } catch (RuntimeException cleanup) {
                error.addSuppressed(cleanup);
            }
            throw error;
        }
        return file;
    }

    public record Download(String url, Instant expiresAt) {}

    public StoredFile metadata(UUID id) {
        return files.findById(id).orElseThrow(() -> new BusinessException(FILE_NOT_FOUND));
    }

    /** 批量元数据只读工具，调用业务必须先完成用途与公开性校验。 */
    public java.util.List<StoredFile> metadata(java.util.List<UUID> ids) {
        return files.findByIds(ids);
    }

    /** 仅生成签名；调用方必须先授权。 */
    public Download inlineUrl(StoredFile file) {
        return new Download(
                objects.imageUrl(file.objectKey(), DOWNLOAD_TTL), Instant.now().plus(DOWNLOAD_TTL));
    }

    public Download downloadUrl(StoredFile file) {
        return new Download(
                objects.downloadUrl(file.objectKey(), file.filename(), DOWNLOAD_TTL),
                Instant.now().plus(DOWNLOAD_TTL));
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        String clean = filename.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        return clean.isEmpty() ? "file" : clean.substring(0, Math.min(180, clean.length()));
    }
}
