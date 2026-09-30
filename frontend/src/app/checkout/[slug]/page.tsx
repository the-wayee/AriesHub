import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowRight, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { conceptCases, formatConceptCredits } from "@/lib/concept-cases";

export const metadata: Metadata = { title: "内容说明" };
export function generateStaticParams() {
  return conceptCases
    .filter((item) => item.creditPrice > 0)
    .map((item) => ({ slug: item.slug }));
}
export default async function CheckoutPage({
  params,
}: PageProps<"/checkout/[slug]">) {
  const { slug } = await params;
  const item = conceptCases.find(
    (entry) => entry.slug === slug && entry.creditPrice > 0,
  );
  if (!item) notFound();
  return (
    <>
      <Link className="hub-back" href={`/cases/${item.slug}`}>
        ← 返回内容
      </Link>
      <header className="hub-page-heading hub-enter">
        <p className="hub-kicker">GO A LITTLE DEEPER</p>
        <h1>
          为下一次实践，
          <br />
          <span>留一点空间。</span>
        </h1>
        <p>先看清内容，再决定是否适合你。</p>
      </header>
      <div className="hub-checkout hub-enter">
        <Image
          src={item.image}
          alt={item.title}
          width={800}
          height={700}
          unoptimized
        />
        <section>
          <p className="hub-kicker">内容确认</p>
          <h2>{item.title}</h2>
          <p>{item.summary}</p>
          <ul>
            {item.deliverables.map((text) => (
              <li key={text}>
                <Check />
                {text}
              </li>
            ))}
          </ul>
          <div>
            <span>解锁所需积分</span>
            <strong>{formatConceptCredits(item.creditPrice)}</strong>
          </div>
          <Button disabled>积分解锁准备中</Button>
          <small>暂未开放解锁，不会扣除积分；充值支付将在后续接入。</small>
          <Link href={`/learn/${item.slug}`} className="hub-inline-link">
            先读免费章节 <ArrowRight />
          </Link>
        </section>
      </div>
    </>
  );
}
