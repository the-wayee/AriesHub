import Image from "next/image";
import Link from "next/link";
import { MotionArticle } from "@/components/page-motion";
import {
  formatConceptCredits,
  type ConceptPublication,
} from "@/lib/concept-publications";

export function PublicationArtwork({
  item,
  priority = false,
}: {
  item: ConceptPublication;
  priority?: boolean;
}) {
  return (
    <div className="publication-artwork">
      <Image
        src={item.image}
        alt=""
        fill
        priority={priority}
        sizes="(max-width: 700px) 100vw, 60vw"
      />
      <span className="publication-artwork-caption" aria-hidden="true">
        ARIESHUB / {item.category.toUpperCase()} <span>↗</span>
      </span>
    </div>
  );
}

export function PublicationCard({
  item,
  index,
  featured = false,
  compact = false,
}: {
  item: ConceptPublication;
  index: number;
  featured?: boolean;
  compact?: boolean;
}) {
  return (
    <MotionArticle
      className={`archive-publication ${featured ? "archive-publication-featured" : ""} ${compact ? "archive-publication-compact" : ""}`}
      direction={index % 2 ? "right" : "left"}
      delay={(index % 3) * 0.06}
    >
      <Link
        href={`/preview/publications/${item.id}`}
        className="archive-publication-link"
      >
        <div className="archive-index">
          <strong>{String(index + 1).padStart(2, "0")}</strong>
          <span>PUBLICATION / {item.categorySlug.toUpperCase()}</span>
        </div>
        <PublicationArtwork item={item} priority={featured} />
        <div className="archive-publication-copy">
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
          <div className="archive-publication-bottom">
            <span>
              {item.date} / {item.category}
            </span>
            <strong>{formatConceptCredits(item.creditPrice)}</strong>
            <span className="archive-publication-action">查看详情&nbsp; →</span>
          </div>
        </div>
      </Link>
    </MotionArticle>
  );
}
