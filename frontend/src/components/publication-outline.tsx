"use client";
import { useEffect, useRef, useState } from "react";
import { List, LockKeyhole } from "lucide-react";
import type { components } from "@/lib/api-schema";
type Chapter = {
  title: string;
  level: number;
  element?: HTMLElement;
  locked?: boolean;
};
type OutlineChapter = components["schemas"]["PublicationChapter"];
/** 完整目录由服务端返回；只有可读章节才关联 DOM 标题，锁定章节不获取私有正文。 */
export function PublicationOutline({
  contentKey,
  chapters: entries,
}: {
  contentKey: string;
  chapters?: OutlineChapter[];
}) {
  const root = useRef<HTMLElement>(null);
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [active, setActive] = useState(0);
  useEffect(() => {
    const article = root.current?.closest("article");
    const headings = Array.from(
      article?.querySelectorAll<HTMLElement>(
        ".hub-detail-body .prose h2, .hub-detail-body .prose h3",
      ) ?? [],
    );
    setChapters(
      entries?.length
        ? entries.map((chapter) => ({
            ...chapter,
            element:
              chapter.headingIndex === null
                ? undefined
                : headings[chapter.headingIndex],
          }))
        : headings.map((element) => ({
            title: element.textContent ?? "",
            level: Number(element.tagName.slice(1)),
            element,
          })),
    );
    let frame = 0;
    const update = () => {
      frame = 0;
      const offset = innerWidth <= 600 ? 200 : 160;
      let index = 0;
      headings.forEach((heading, current) => {
        if (heading.getBoundingClientRect().top <= offset) index = current;
      });
      setActive(
        entries?.length
          ? entries.findIndex(
              (chapter) => !chapter.locked && chapter.headingIndex === index,
            )
          : index,
      );
    };
    const schedule = () => {
      if (!frame) frame = requestAnimationFrame(update);
    };
    update();
    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("resize", schedule);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("scroll", schedule);
      window.removeEventListener("resize", schedule);
    };
  }, [contentKey, entries]);
  return (
    <nav ref={root} className="publication-outline" aria-label="章节目录">
      <header>
        <h2>
          <List size={18} />
          章节目录
        </h2>
        <span>{chapters.length} 节</span>
      </header>
      {chapters.length ? (
        <ol>
          {chapters.map((chapter, index) => (
            <li key={index} data-level={chapter.level}>
              <button
                type="button"
                disabled={chapter.locked || !chapter.element}
                aria-current={active === index ? "location" : undefined}
                onClick={() => {
                  if (chapter.locked || !chapter.element) return;
                  const top =
                    scrollY +
                    chapter.element.getBoundingClientRect().top -
                    (innerWidth <= 600 ? 190 : 140);
                  window.scrollTo({
                    top,
                    behavior: matchMedia("(prefers-reduced-motion: reduce)")
                      .matches
                      ? "instant"
                      : "smooth",
                  });
                  chapter.element.tabIndex = -1;
                  chapter.element.focus({ preventScroll: true });
                }}
              >
                <span>{String(index + 1).padStart(2, "0")}</span>
                {chapter.title}
                {chapter.locked && (
                  <LockKeyhole size={13} aria-label="解锁后可读" />
                )}
              </button>
            </li>
          ))}
        </ol>
      ) : (
        <p>作者尚未设置章节标题。</p>
      )}
      {chapters.some((chapter) => chapter.locked) && (
        <small>
          <LockKeyhole size={13} />
          解锁后可阅读全部章节
        </small>
      )}
    </nav>
  );
}
