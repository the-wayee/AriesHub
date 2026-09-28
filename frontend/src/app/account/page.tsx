import type { Metadata } from "next";
import { AccountPanel } from "@/components/account-panel";

export const metadata: Metadata = { title: "我的账号" };

export default function AccountPage() {
  return (
    <div className="account-page">
      <header className="page-heading">
        <p className="eyebrow">YOUR SPACE</p>
        <h1>我的 AriesHub</h1>
        <p>这里将逐步汇集你的案例、收藏、订单和社区权益。</p>
      </header>
      <AccountPanel />
      <section className="account-roadmap" aria-labelledby="roadmap-title">
        <p className="eyebrow">WHAT’S NEXT</p>
        <h2 id="roadmap-title">你的学习空间正在生长</h2>
        <div>
          <p>
            <span>01</span> 收藏感兴趣的案例
          </p>
          <p>
            <span>02</span> 查看已购买的素材
          </p>
          <p>
            <span>03</span> 记录实践与作品
          </p>
        </div>
      </section>
    </div>
  );
}
