import Link from "next/link";
import type { CaseSummary } from "@/lib/catalog-types";

const styles: Record<
  string,
  { theme: string; word: string; illustration: string }
> = {
  "ai-ppt": {
    theme: "presentation",
    word: "CREATE",
    illustration: "把想法\n讲清楚。",
  },
  coding: {
    theme: "code",
    word: "BUILD",
    illustration: "> 一个想法\n  一次实践\n  一个作品_",
  },
  automation: {
    theme: "workflow",
    word: "SIMPLIFY",
    illustration: "收集 → 整理\n       ↓\n     变成成果",
  },
};

export function priceLabel(item: CaseSummary) {
  if (item.accessType === "FREE") return "免费阅读";
  return `${item.isDemo ? "演示价格 " : ""}${new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: item.currency,
  }).format(item.priceMinor / 100)}`;
}

export function CaseCard({
  item,
  index,
}: {
  item: CaseSummary;
  index: number;
}) {
  const style = styles[item.categorySlug] ?? {
    theme: "presentation",
    word: "EXPLORE",
    illustration: "从好奇\n到作品。",
  };
  return (
    <article className="case-card">
      <Link className="case-card-link" href={`/cases/${item.slug}`}>
        <div className={`direction-art ${style.theme}`} aria-hidden="true">
          <div className="art-label">
            <span>{style.word}</span>
            <span>/{String(index + 1).padStart(2, "0")}</span>
          </div>
          <div className="art-text">{style.illustration}</div>
        </div>
        <div className="card-meta">
          <span>{item.categoryName}</span>
          {item.isDemo && <span className="demo-badge">演示案例</span>}
        </div>
        <h3>{item.title}</h3>
        <p className="direction-description">{item.summary}</p>
        <div className="card-bottom">
          <span>{priceLabel(item)}</span>
          <span>
            查看案例 <span aria-hidden="true">↗</span>
          </span>
        </div>
      </Link>
    </article>
  );
}
