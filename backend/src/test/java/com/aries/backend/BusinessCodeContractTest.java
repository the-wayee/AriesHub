package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.aries.backend.catalog.application.query.PublicationSort;
import com.aries.backend.catalog.domain.model.Publication.AccessType;
import com.aries.backend.catalog.domain.model.Publication.PublicationType;
import com.aries.backend.catalog.domain.model.PublicationLibraryKind;
import com.aries.backend.discussion.application.query.CommentPageQuery.Order;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/** 注解只能使用编译期常量，验证校验编码与枚举同步，防止新增值后接口仍拒绝它。 */
class BusinessCodeContractTest {
    @Test
    void validationRulesAndDefaultsMatchDomainCodes() {
        assertCodes(AccessType.VALID_VALUES, AccessType.values());
        assertCodes(PublicationType.VALID_VALUES, PublicationType.values());
        assertCodes(PublicationLibraryKind.VALID_VALUES, PublicationLibraryKind.values());
        assertCodes(PublicationSort.VALID_VALUES, PublicationSort.values());
        assertCodes(Order.VALID_VALUES, Order.values());
        assertThat(PublicationLibraryKind.DEFAULT_CODE)
                .isEqualTo(PublicationLibraryKind.BOOKMARK.name());
    }

    private void assertCodes(String rule, Enum<?>[] values) {
        List<String> names = Arrays.stream(values).map(Enum::name).toList();
        assertThat(rule.split("\\|")).containsExactlyInAnyOrderElementsOf(names);
    }
}
