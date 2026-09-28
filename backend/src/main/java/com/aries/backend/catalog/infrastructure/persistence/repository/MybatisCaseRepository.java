package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.domain.model.CaseStudy;
import com.aries.backend.catalog.domain.repository.CaseRepository;
import com.aries.backend.catalog.infrastructure.persistence.converter.CaseConverter;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CaseMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.CasePO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/** 领域仓储的数据库实现：简单查询用类型安全的 Lambda 条件构造器。 */
@Repository
@RequiredArgsConstructor
public class MybatisCaseRepository implements CaseRepository {
    private final CaseMapper mapper;

    @Override
    public Optional<CaseStudy> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(CaseConverter::toDomain);
    }

    @Override
    public Optional<CaseStudy> findBySlug(String slug) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<CasePO>lambdaQuery()
                .eq(CasePO::getSlug, slug))).map(CaseConverter::toDomain);
    }
}
