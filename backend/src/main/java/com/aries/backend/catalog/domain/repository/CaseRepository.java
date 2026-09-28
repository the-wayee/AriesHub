package com.aries.backend.catalog.domain.repository;

import com.aries.backend.catalog.domain.model.CaseStudy;
import java.util.Optional;

/** 案例聚合的仓储契约；领域层只认识聚合，不认识 Mapper 或持久化对象。 */
public interface CaseRepository {
    Optional<CaseStudy> findById(long id);
    Optional<CaseStudy> findBySlug(String slug);
}
