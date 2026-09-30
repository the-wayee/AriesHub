package com.aries.backend.catalog.infrastructure.persistence.repository;

import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.infrastructure.persistence.mapper.CreditOfferMapper;
import com.aries.backend.catalog.infrastructure.persistence.po.CreditOfferPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Publication 聚合持久化时使用的积分报价仓储；积分账户和扣减仍由 credits 模块负责。 */
@Repository
@RequiredArgsConstructor
class MybatisCreditOfferRepository {
    private static final String TARGET_TYPE = "PUBLICATION";
    private final CreditOfferMapper mapper;

    long priceFor(String slug) {
        CreditOfferPO offer = find(slug, "ACTIVE");
        return offer == null ? 0 : offer.getCreditPrice();
    }

    void save(String previousSlug, Publication publication) {
        if (previousSlug != null && !previousSlug.equals(publication.getSlug())) {
            mapper.update(Wrappers.<CreditOfferPO>lambdaUpdate()
                    .eq(CreditOfferPO::getTargetType, TARGET_TYPE)
                    .eq(CreditOfferPO::getTargetKey, previousSlug)
                    .set(CreditOfferPO::getTargetKey, publication.getSlug()));
        }
        if (publication.getAccessType() == Publication.AccessType.FREE) {
            mapper.update(Wrappers.<CreditOfferPO>lambdaUpdate()
                    .eq(CreditOfferPO::getTargetType, TARGET_TYPE)
                    .eq(CreditOfferPO::getTargetKey, publication.getSlug())
                    .set(CreditOfferPO::getStatus, "INACTIVE"));
            return;
        }
        CreditOfferPO offer = find(publication.getSlug(), null);
        if (offer == null) {
            offer = new CreditOfferPO();
            offer.setTargetType(TARGET_TYPE);
            offer.setTargetKey(publication.getSlug());
        }
        offer.setCreditPrice(publication.getCreditPrice());
        offer.setStatus("ACTIVE");
        mapper.insertOrUpdate(offer);
    }

    private CreditOfferPO find(String slug, String status) {
        var query = Wrappers.<CreditOfferPO>lambdaQuery()
                .eq(CreditOfferPO::getTargetType, TARGET_TYPE)
                .eq(CreditOfferPO::getTargetKey, slug);
        if (status != null) query.eq(CreditOfferPO::getStatus, status);
        return mapper.selectOne(query);
    }
}
