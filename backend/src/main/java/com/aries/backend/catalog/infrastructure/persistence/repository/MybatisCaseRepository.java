package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.domain.model.CaseStudy;
import com.aries.backend.catalog.domain.repository.CaseRepository;
import com.aries.backend.catalog.infrastructure.persistence.converter.CaseConverter;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CaseMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CaseContentMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.CaseContentPO;
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
    private final CaseContentMapper contents;

    @Override
    public Optional<CaseStudy> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(CaseConverter::toDomain);
    }

    @Override
    public Optional<CaseStudy> findBySlug(String slug) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<CasePO>lambdaQuery()
                .eq(CasePO::getSlug, slug))).map(CaseConverter::toDomain);
    }

    @Override
    public Optional<CaseStudy> findForEditing(long id) {
        CasePO row = mapper.selectById(id);
        return row == null ? Optional.empty() :
                Optional.of(CaseConverter.toDomain(row, contents.selectById(id)));
    }

    @Override
    public CaseStudy save(CaseStudy study) {
        CasePO row = CaseConverter.toPO(study);
        if (study.getId() == 0) mapper.insert(row);
        else mapper.updateById(row);

        CaseStudy.Content content = study.getContent();
        if (content != null) {
            CaseContentPO body = CaseConverter.toContentPO(row.getId(), content);
            if (contents.selectById(row.getId()) == null) contents.insert(body);
            else contents.updateById(body);
        }
        return study.toBuilder().id(row.getId()).build();
    }
}
