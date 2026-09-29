package com.aries.backend.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;

class CaseStudyTest {
    private final CaseStudy.Content content = new CaseStudy.Content(
            "预览", "完整正文", "准备", "交付", "1.0");

    private CaseStudy.Draft draft(CaseStudy.Content body) {
        return new CaseStudy.Draft(1, "case", "标题", "摘要",
                CaseStudy.AccessType.FREE, 0, body);
    }

    @Test
    void createStartsAsDraftAndCannotBeReadPublicly() {
        CaseStudy study = CaseStudy.create(draft(content));
        assertThat(study.getStatus()).isEqualTo(CaseStudy.PublicationStatus.DRAFT);
        assertThat(study.getCurrency()).isEqualTo("CNY");
        assertThat(study.isPubliclyVisible()).isFalse();
    }

    @Test
    void publishRequiresFullBodyAndSetsTimestamp() {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-29T10:00:00Z");
        CaseStudy incomplete = CaseStudy.create(draft(new CaseStudy.Content("预览", " ", "准备", "交付", "1.0")));
        assertThatThrownBy(() -> incomplete.publish(now)).isInstanceOf(CaseStudy.MissingContent.class);

        CaseStudy published = CaseStudy.create(draft(content)).publish(now);
        assertThat(published.getPublishedAt()).isEqualTo(now);
        assertThat(published.isPubliclyVisible()).isTrue();
        assertThat(published.allowsPublicReading()).isTrue();
        assertThat(published.archive().isPubliclyVisible()).isFalse();
    }

    @Test
    void editingPublishedCasePreservesPublicationState() {
        CaseStudy published = CaseStudy.create(draft(content))
                .publish(OffsetDateTime.parse("2026-09-29T10:00:00Z"));
        CaseStudy edited = published.edit(new CaseStudy.Draft(1, "new-slug", "新标题", "新摘要",
                CaseStudy.AccessType.PAID, 1990, content));
        assertThat(edited.getStatus()).isEqualTo(CaseStudy.PublicationStatus.PUBLISHED);
        assertThat(edited.getPublishedAt()).isEqualTo(published.getPublishedAt());
        assertThat(edited.allowsPublicReading()).isFalse();
        assertThatThrownBy(() -> published.edit(draft(
                new CaseStudy.Content("预览", " ", "准备", "交付", "1.0"))))
                .isInstanceOf(CaseStudy.MissingContent.class);
    }
}
