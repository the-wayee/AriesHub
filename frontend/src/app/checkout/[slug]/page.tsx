import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { CaseArtwork } from "@/components/case-card";
import { Button } from "@/components/ui/button";
import { conceptCases, formatConceptPrice } from "@/lib/concept-cases";

export const metadata: Metadata = { title: "确认案例" };

export function generateStaticParams() {
  return conceptCases
    .filter((item) => item.price > 0)
    .map((item) => ({ slug: item.slug }));
}

export default async function CheckoutPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const item = conceptCases.find(
    (entry) => entry.slug === slug && entry.price > 0,
  );
  if (!item) notFound();

  return (
    <div className="concept-checkout">
      <Link className="concept-back" href={`/cases/${item.slug}`}>
        ← 返回案例
      </Link>
      <div className="checkout-grid">
        <div className="checkout-story">
          <p className="mono-label">09 / CHECKOUT</p>
          <h1>确认这份案例</h1>
          <p>先看清内容与交付，再决定是否适合自己的实践。</p>
          <div className="checkout-case">
            <CaseArtwork item={item} priority />
            <div>
              <p className="mono-label">CASE STUDY</p>
              <h2>{item.title}</h2>
              <p>{item.summary}</p>
              <h3>你将获得</h3>
              <ol>
                {item.deliverables.map((entry, index) => (
                  <li key={entry}>
                    <span>{String(index + 1).padStart(2, "0")}</span>
                    {entry}
                  </li>
                ))}
              </ol>
            </div>
          </div>
          <div className="checkout-terms">
            <h2>使用条件与说明</h2>
            <p>
              本页是前端设计原型。支付、订单、内容授权与下载尚未接入；具体条款将在业务方案确定后呈现。
            </p>
          </div>
        </div>
        <aside className="checkout-summary" aria-label="案例摘要">
          <p className="mono-label">ORDER SUMMARY / 01</p>
          <div className="checkout-summary-case">
            <CaseArtwork item={item} />
            <strong>{item.title}</strong>
            <span>{formatConceptPrice(item.price)}</span>
          </div>
          <div className="checkout-total">
            <span>案例价格</span>
            <strong>{formatConceptPrice(item.price)}</strong>
          </div>
          <div className="checkout-total checkout-total-final">
            <span>应付总额</span>
            <strong>{formatConceptPrice(item.price)}</strong>
          </div>
          <Button className="checkout-disabled" disabled>
            支付功能准备中
          </Button>
          <p className="checkout-note">
            当前仅展示页面设计，不会生成订单或扣款。
          </p>
          <div className="checkout-methods">
            <p className="mono-label">PAYMENT METHODS</p>
            <span>支付方式将在正式接入时显示。</span>
          </div>
        </aside>
      </div>
      <div className="checkout-steps">
        <p className="mono-label">NEXT STEPS</p>
        <span>01 / 确认案例</span>
        <span>02 / 完成支付</span>
        <span>03 / 阅读与下载</span>
      </div>
    </div>
  );
}
