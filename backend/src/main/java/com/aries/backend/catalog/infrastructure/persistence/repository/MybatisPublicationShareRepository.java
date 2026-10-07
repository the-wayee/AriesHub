package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.application.port.PublicationShareRepository;
import com.aries.backend.catalog.infrastructure.persistence.mapper.PublicationShareMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.PublicationSharePO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

/** 应用用例先持有文章锁，再查询/创建；数据库联合唯一约束作为并发兜底。 */
@Repository
@RequiredArgsConstructor
public class MybatisPublicationShareRepository implements PublicationShareRepository {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private final PublicationShareMapper mapper;

    public Share getOrCreate(long user, long publication) {
        PublicationSharePO po =
                mapper.selectOne(
                        Wrappers.<PublicationSharePO>lambdaQuery()
                                .eq(PublicationSharePO::getUserId, user)
                                .eq(PublicationSharePO::getPublicationId, publication));
        if (po == null) {
            byte[] bytes = new byte[TOKEN_BYTES];
            RANDOM.nextBytes(bytes);
            po = new PublicationSharePO();
            po.setUserId(user);
            po.setPublicationId(publication);
            po.setToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
            po.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            mapper.insert(po);
        }
        return convert(po);
    }

    public Optional<Share> find(String token) {
        return Optional.ofNullable(
                        mapper.selectOne(
                                Wrappers.<PublicationSharePO>lambdaQuery()
                                        .eq(PublicationSharePO::getToken, token)))
                .map(this::convert);
    }

    private Share convert(PublicationSharePO po) {
        return new Share(po.getId(), po.getPublicationId(), po.getUserId(), po.getToken());
    }
}
