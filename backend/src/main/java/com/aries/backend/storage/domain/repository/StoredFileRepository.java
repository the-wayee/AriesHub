package com.aries.backend.storage.domain.repository;

import com.aries.backend.storage.domain.model.StoredFile;
import java.util.Optional;
import java.util.UUID;

public interface StoredFileRepository {
    void save(StoredFile file);
    Optional<StoredFile> findById(UUID id);
}
