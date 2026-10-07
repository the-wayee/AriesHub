package com.aries.backend.catalog.application.service;

import com.aries.backend.catalog.application.view.CatalogViews.Chapter;
import com.aries.backend.catalog.domain.model.Publication;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Heading;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/** 从 Markdown 语法树提取目录，代码块和 HTML 注释中的伪标题不会成为章节。 */
public final class PublicationOutline {
    private static final Parser PARSER = Parser.builder().build();

    private PublicationOutline() {}

    public static List<Chapter> chapters(Publication publication) {
        List<Chapter> full = headings(publication.getContent().fullMarkdown());
        if (publication.allowsPublicReading()) return full;
        List<Chapter> preview = headings(publication.getContent().previewMarkdown());
        List<Chapter> result = new ArrayList<>();
        int visibleIndex = 0;
        // 旧内容可以有独立试读；仅匹配实际试读标题，找不到的章节保持锁定。
        for (Chapter chapter : full) {
            boolean visible =
                    visibleIndex < preview.size()
                            && chapter.title().equals(preview.get(visibleIndex).title())
                            && chapter.level() == preview.get(visibleIndex).level();
            result.add(
                    new Chapter(
                            chapter.title(),
                            chapter.level(),
                            !visible,
                            visible ? visibleIndex++ : null));
        }
        return List.copyOf(result);
    }

    private static List<Chapter> headings(String markdown) {
        List<Chapter> result = new ArrayList<>();
        PARSER.parse(markdown == null ? "" : markdown)
                .accept(
                        new AbstractVisitor() {
                            @Override
                            public void visit(Heading heading) {
                                if (heading.getLevel() != 2 && heading.getLevel() != 3) return;
                                StringBuilder title = new StringBuilder();
                                heading.accept(
                                        new AbstractVisitor() {
                                            @Override
                                            public void visit(Text text) {
                                                title.append(text.getLiteral());
                                            }

                                            @Override
                                            public void visit(Code code) {
                                                title.append(code.getLiteral());
                                            }
                                        });
                                result.add(
                                        new Chapter(
                                                title.toString(),
                                                heading.getLevel(),
                                                false,
                                                result.size()));
                            }
                        });
        return List.copyOf(result);
    }
}
