package com.aries.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.aries.backend.catalog.application.service.PublicationOutline;
import com.aries.backend.catalog.application.view.CatalogViews.Chapter;
import com.aries.backend.catalog.domain.model.Publication;

import org.junit.jupiter.api.Test;

import java.util.List;

class PublicationOutlineTest {
    private Publication article(boolean free, String full, String preview) {
        return Publication.builder()
                .status(Publication.PublicationStatus.PUBLISHED)
                .deliveryStatus(Publication.DeliveryStatus.AVAILABLE)
                .accessType(free ? Publication.AccessType.FREE : Publication.AccessType.CREDIT)
                .content(new Publication.Content(preview, full, "v1"))
                .build();
    }

    @Test
    void exposesAllTitlesButOnlyPreviewChaptersCanNavigate() {
        String preview = "## 开始 **练习**\n\n公开段落\n";
        String full =
                preview
                        + "\n"
                        + "<!-- arieshub:paid -->\n\n"
                        + "### 第二课\n\n"
                        + "秘密正文 media:private-key\n\n"
                        + "## 开始 **练习**";
        List<Chapter> chapters = PublicationOutline.chapters(article(false, full, preview));
        assertThat(chapters)
                .containsExactly(
                        new Chapter("开始 练习", 2, false, 0),
                        new Chapter("第二课", 3, true, null),
                        new Chapter("开始 练习", 2, true, null));
        assertThat(chapters.toString()).doesNotContain("秘密正文", "private-key");
        assertThat(PublicationOutline.chapters(article(true, full, preview)))
                .allMatch(chapter -> !chapter.locked() && chapter.headingIndex() != null);
    }

    @Test
    void ignoresCodeAndCommentsAndSupportsSetextHeadings() {
        String full =
                "```md\n"
                    + "## 伪标题\n"
                    + "```\n\n"
                    + "<!--\n"
                    + "## 隐藏注释\n"
                    + "-->\n\n"
                    + "实际章节\n"
                    + "---\n\n"
                    + "### 使用 `API` 与 [示例](https://example.com)";
        assertThat(PublicationOutline.chapters(article(true, full, "")))
                .extracting(Chapter::title)
                .containsExactly("实际章节", "使用 API 与 示例");
    }

    @Test
    void noPreviewLocksEveryChapter() {
        assertThat(PublicationOutline.chapters(article(false, "## 章节\n\n正文", "")))
                .containsExactly(new Chapter("章节", 2, true, null));
    }
}
