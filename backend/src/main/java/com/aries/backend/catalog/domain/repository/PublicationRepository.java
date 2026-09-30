package com.aries.backend.catalog.domain.repository;

import com.aries.backend.catalog.domain.model.Publication;
import java.util.Optional;

/** 发布内容聚合的仓储契约；领域层只认识聚合，不认识 Mapper 或持久化对象。 */
public interface PublicationRepository {
    Optional<Publication> findById(long id);
    Optional<Publication> findBySlug(String slug);
    Optional<Publication> findForEditing(long id);
    Publication save(Publication publication);
}
