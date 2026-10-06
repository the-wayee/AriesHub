package com.aries.backend.storage.application.service;

import com.aries.backend.storage.application.port.StorageIdentityProvider;
import com.aries.backend.shared.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;
import static com.aries.backend.storage.application.exception.StorageErrorCode.FILE_NOT_FOUND;

/** 通用私有文件下载用例；内容权益下载以后由对应业务领域提供。 */
@Service
@RequiredArgsConstructor
public class FileDownloadService {
    private final FileStorageService files;
    private final StorageIdentityProvider identity;
    public FileStorageService.Download download(UUID id) {
        long owner = identity.currentUserId();
        var file = files.metadata(id);
        if (file.ownerId() != owner) throw new BusinessException(FILE_NOT_FOUND);
        return files.downloadUrl(file);
    }
}
