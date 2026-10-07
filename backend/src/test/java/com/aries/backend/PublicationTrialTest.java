package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.aries.backend.catalog.domain.model.PublicationTrial;

import org.junit.jupiter.api.Test;

class PublicationTrialTest {
    @Test
    void boundaryOverridesLegacyPreviewAndExcludesPrivateMedia() {
        String publicPart = "![image](media:11111111-1111-1111-1111-111111111111)";
        String privatePart = "![video](media:22222222-2222-2222-2222-222222222222)";
        String result =
                PublicationTrial.preview(
                        "CREDIT",
                        publicPart + "\n" + PublicationTrial.BOUNDARY + "\n" + privatePart,
                        privatePart);
        assertThat(result).isEqualTo(publicPart).doesNotContain("22222222");
        assertThat(PublicationTrial.preview("CREDIT", privatePart, null)).isEmpty();
        assertThat(PublicationTrial.preview("CREDIT", privatePart, "旧版试读")).isEqualTo("旧版试读");
    }
}
