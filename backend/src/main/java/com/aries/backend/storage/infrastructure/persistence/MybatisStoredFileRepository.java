package com.aries.backend.storage.infrastructure.persistence;

import com.aries.backend.storage.domain.model.StoredFile;
import com.aries.backend.storage.domain.repository.StoredFileRepository;
import com.aries.backend.storage.infrastructure.persistence.mapper.StoredFileMapper;
import com.aries.backend.storage.infrastructure.persistence.po.StoredFilePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MybatisStoredFileRepository implements StoredFileRepository {
    private final StoredFileMapper mapper;
    @Override public void save(StoredFile file) {
        StoredFilePO po = new StoredFilePO();
        po.setId(file.id().toString()); po.setOwnerId(file.ownerId()); po.setPurpose(file.purpose().name());
        po.setObjectKey(file.objectKey()); po.setFilename(file.filename());
        po.setContentType(file.contentType()); po.setSize(file.size());
        mapper.insert(po);
    }
    @Override public Optional<StoredFile> findById(UUID id) {
        return Optional.ofNullable(mapper.selectById(id.toString())).map(po -> new StoredFile(UUID.fromString(po.getId()),
                po.getOwnerId(), StoredFile.Purpose.valueOf(po.getPurpose()), po.getObjectKey(),
                po.getFilename(), po.getContentType(), po.getSize()));
    }
}
