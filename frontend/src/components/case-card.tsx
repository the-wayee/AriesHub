import Image from "next/image";
import Link from "next/link";
import { MotionArticle } from "@/components/page-motion";
import { formatConceptCredits, type ConceptCase } from "@/lib/concept-cases";

export function CaseArtwork({
  item,
  priority = false,
}: {
  item: ConceptCase;
  priority?: boolean;
}) {
  return (
    <div className="case-artwork">
      <Image
        src={item.image}
        alt=""
        fill
        priority={priority}
        sizes="(max-width: 700px) 100vw, 60vw"
      />
      <span className="case-artwork-caption" aria-hidden="true">
        ARIESHUB / {item.category.toUpperCase()} <span>↗</span>
      </span>
    </div>
  );
}

export function CaseCard({
  item,
  index,
  featured = false,
  compact = false,
}: {
  item: ConceptCase;
  index: number;
  featured?: boolean;
  compact?: boolean;
}) {
  return (
    <MotionArticle
      className={`archive-case ${featured ? "archive-case-featured" : ""} ${compact ? "archive-case-compact" : ""}`}
      direction={index % 2 ? "right" : "left"}
      delay={(index % 3) * 0.06}
    >
      <Link href={`/cases/${item.slug}`} className="archive-case-link">
        <div className="archive-index">
          <strong>{String(index + 1).padStart(2, "0")}</strong>
          <span>CASE / {item.categorySlug.toUpperCase()}</span>
        </div>
        <CaseArtwork item={item} priority={featured} />
        <div className="archive-case-copy">
          <span className="archive-access">
            {item.creditPrice === 0 ? "免费内容" : "积分内容"}
          </span>
          <h2>{item.title}</h2>
          <p>{item.summary}</p>
          {featured && (
            <div className="archive-deliverables">
              <small>你将获得</small>
              <span>{item.deliverables.join(" / ")}</span>
            </div>
          )}
          <div className="archive-case-bottom">
            <span>
              {item.date} / {item.category}
            </span>
            <strong>{formatConceptCredits(item.creditPrice)}</strong>
            <span className="archive-case-action">查看详情&nbsp; →</span>
          </div>
        </div>
      </Link>
    </MotionArticle>
  );
}
