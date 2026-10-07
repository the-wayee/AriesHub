package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.domain.repository.PublicationRepository;
import com.aries.backend.catalog.infrastructure.persistence.converter.PublicationConverter;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationContentMapper;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationContentPO;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationPO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 领域仓储的数据库实现：简单查询用类型安全的 Lambda 条件构造器。 */
@Repository
@RequiredArgsConstructor
public class MybatisPublicationRepository implements PublicationRepository {
    private final PublicationMapper mapper;
    private final PublicationContentMapper contents;

    @Override
    public Optional<Publication> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Publication> findForEditing(long id) {
        PublicationPO row = mapper.selectById(id);
        return row == null
                ? Optional.empty()
                : Optional.of(PublicationConverter.toDomain(row, contents.selectById(id)));
    }

    @Override
    public Publication save(Publication publication) {
        PublicationPO row = PublicationConverter.toPO(publication);
        if (publication.getId() == 0) mapper.insert(row);
        else mapper.updateById(row);

        Publication.Content content = publication.getContent();
        if (content != null) {
            PublicationContentPO body = PublicationConverter.toContentPO(row.getId(), content);
            if (contents.selectById(row.getId()) == null) contents.insert(body);
            else contents.updateById(body);
        }
        return publication.toBuilder().id(row.getId()).build();
    }

    private Publication toDomain(PublicationPO row) {
        return PublicationConverter.toDomain(row, null);
    }
}
