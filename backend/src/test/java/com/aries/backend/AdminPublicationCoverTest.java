package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aries.backend.catalog.application.port.AdminCatalogReadPort;
import com.aries.backend.catalog.application.port.CategoryWritePort;
import com.aries.backend.catalog.application.port.PublicationCoverPort;
import com.aries.backend.catalog.application.port.PublicationMediaPort.SignedUrl;
import com.aries.backend.catalog.application.service.AdminCatalogService;
import com.aries.backend.catalog.application.service.PublicationMediaService;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationListItem;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationSummary;
import com.aries.backend.catalog.domain.repository.PublicationRepository;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 管理列表一次批量签名，相同封面只读取一次；无封面文章仍正常返回。 */
class AdminPublicationCoverTest {
    @Test
    void signsDistinctCoversAndReturnsUrlsInListResponse() {
        AdminCatalogReadPort reads = mock(AdminCatalogReadPort.class);
        PublicationCoverPort covers = mock(PublicationCoverPort.class);
        AdminCatalogService service =
                new AdminCatalogService(
                        reads,
                        covers,
                        mock(CategoryWritePort.class),
                        mock(PublicationRepository.class),
                        mock(PublicationMediaService.class));
        when(reads.publications())
                .thenReturn(
                        List.of(summary("1", "cover"), summary("2", "cover"), summary("3", null)));
        SignedUrl signed =
                new SignedUrl(
                        "https://storage.example/cover?signature=test",
                        Instant.parse("2099-01-01T00:00:00Z"));
        Map<String, SignedUrl> urls = new HashMap<>();
        urls.put("cover", signed);
        when(covers.sign(List.of("cover"))).thenReturn(urls);
        List<AdminPublicationListItem> result = service.publications();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).cover()).isEqualTo(signed);
        assertThat(result.get(1).cover()).isEqualTo(signed);
        assertThat(result.get(2).cover()).isNull();
        assertThat(result.get(0).coverFileId()).isEqualTo("cover");
        verify(covers).sign(List.of("cover"));
    }

    private AdminPublicationSummary summary(String id, String cover) {
        return new AdminPublicationSummary(
                id,
                "slug-" + id,
                "文章",
                "分类",
                "#4967A9",
                "ARTICLE",
                "FREE",
                0,
                "DRAFT",
                "AVAILABLE",
                null,
                null,
                cover,
                false);
    }
}
