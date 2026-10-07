package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aries.backend.shared.infrastructure.config.ConfiguredSiteAddress;

import org.junit.jupiter.api.Test;

/** 域名由后端配置，环境切换不影响文章路径。 */
class SiteAddressTest {
    @Test
    void originChangesWithoutChangingArticlePath() {
        assertThat(
                        new ConfiguredSiteAddress("http://localhost:3200")
                                .absolutePath("/publications/11"))
                .isEqualTo("http://localhost:3200/publications/11");
        assertThat(
                        new ConfiguredSiteAddress("https://community.example.com/")
                                .absolutePath("/publications/11"))
                .isEqualTo("https://community.example.com/publications/11");
    }

    @Test
    void invalidOriginsFailEarly() {
        for (String origin :
                new String[] {
                    "javascript:alert(1)",
                    "https://user:pass@example.com",
                    "https://example.com/articles",
                    "https://example.com?token=1"
                }) {
            assertThatThrownBy(() -> new ConfiguredSiteAddress(origin))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
