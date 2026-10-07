package com.aries.backend.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;

class PublicationTest {
    private final Publication.Content content = new Publication.Content(
            "预览", "完整正文", "1.0");

    private Publication.Draft draft(Publication.Content body) {
        return new Publication.Draft(1, "case", "标题", "摘要",
                Publication.PublicationType.CASE_STUDY, Publication.AccessType.FREE, 0, body);
    }

    @Test
    void createStartsAsDraftAndCannotBeReadPublicly() {
        Publication publication = Publication.create(draft(content));
        assertThat(publication.getStatus()).isEqualTo(Publication.PublicationStatus.DRAFT);
        assertThat(publication.getPublicationType()).isEqualTo(Publication.PublicationType.CASE_STUDY);
        assertThat(publication.isPubliclyVisible()).isFalse();
    }

    @Test
    void publishRequiresFullBodyAndSetsTimestamp() {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-29T10:00:00Z");
        Publication incomplete = Publication.create(draft(new Publication.Content("预览", " ", "1.0")));
        assertThatThrownBy(() -> incomplete.publish(now)).isInstanceOf(Publication.MissingContent.class);

        Publication published = Publication.create(draft(content)).publish(now);
        assertThat(published.getPublishedAt()).isEqualTo(now);
        assertThat(published.isPubliclyVisible()).isTrue();
        assertThat(published.allowsPublicReading()).isTrue();
        assertThat(published.archive().isPubliclyVisible()).isFalse();
    }

    @Test
    void editingPublishedPublicationPreservesPublicationState() {
        Publication published = Publication.create(draft(content))
                .publish(OffsetDateTime.parse("2026-09-29T10:00:00Z"));
        Publication edited = published.edit(new Publication.Draft(1, "new-slug", "新标题", "新摘要",
                Publication.PublicationType.ARTICLE, Publication.AccessType.CREDIT, 199, content));
        assertThat(edited.getStatus()).isEqualTo(Publication.PublicationStatus.PUBLISHED);
        assertThat(edited.getPublishedAt()).isEqualTo(published.getPublishedAt());
        assertThat(edited.allowsPublicReading()).isFalse();
        assertThatThrownBy(() -> published.edit(draft(
                new Publication.Content("预览", " ", "1.0"))))
                .isInstanceOf(Publication.MissingContent.class);
    }
}
