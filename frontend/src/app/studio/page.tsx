import type { Metadata } from "next";
import Link from "next/link";
import { CaseArtwork } from "@/components/case-card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { conceptCases, formatConceptPrice } from "@/lib/concept-cases";

export const metadata: Metadata = { title: "创作者工作台" };

export default function StudioPage() {
  const selected = conceptCases[1];
  return (
    <div className="concept-studio">
      <aside className="studio-nav">
        <h1>工作台</h1>
        <p className="mono-label">CREATOR CONSOLE</p>
        <p>Good tools for a more independent you.</p>
        <nav aria-label="工作台导航">
          <span>01　概览</span>
          <strong>02　案例管理</strong>
          <span>03　资源文件</span>
          <span>04　订单与退款</span>
          <span>05　讨论管理</span>
        </nav>
        <small>
          AriesHub
          <br />
          For Independent Creators.
        </small>
      </aside>
      <div className="studio-list">
        <header>
          <p className="mono-label">02 / CONTENT</p>
          <h2>内容</h2>
          <p>管理你的 AI 案例、付费内容与资源文件。</p>
        </header>
        <div className="studio-list-bar">
          <span>全部 {conceptCases.length}　　已发布 3　　草稿 1</span>
          <Button disabled>＋ 新建案例</Button>
        </div>
        <div className="studio-table-head">
          <span>标题</span>
          <span>状态</span>
          <span>价格</span>
          <span>操作</span>
        </div>
        {conceptCases.map((item, index) => (
          <div
            className={`studio-row ${index === 1 ? "selected" : ""}`}
            key={item.slug}
          >
            <div className="studio-row-title">
              <CaseArtwork item={item} />
              <div>
                <strong>{item.title}</strong>
                <small>{item.summary}</small>
              </div>
            </div>
            <span>{index === 1 ? "● 草稿" : "● 已发布"}</span>
            <span>{formatConceptPrice(item.price)}</span>
            <Link href={`/cases/${item.slug}`}>预览 ↗</Link>
          </div>
        ))}
        <p className="community-prototype-note">
          工作台为前端设计原型；新建、编辑和发布操作暂未连接内容系统。
        </p>
      </div>
      <aside className="studio-editor">
        <div className="studio-editor-top">
          <span>编辑案例</span>
          <Link href={`/cases/${selected.slug}`}>预览页面 ↗</Link>
        </div>
        <h2>{selected.title}</h2>
        <p>最近编辑 · 设计示例</p>
        <nav aria-label="编辑区域">
          <strong>公开介绍</strong>
          <span>付费正文</span>
          <span>资源文件</span>
          <span>售卖设置</span>
        </nav>
        <form>
          <Label htmlFor="studio-cover">封面图</Label>
          <div id="studio-cover" className="studio-editor-cover">
            <CaseArtwork item={selected} />
            <Button type="button" variant="outline" disabled>
              更换封面
            </Button>
          </div>
          <Label htmlFor="studio-title">标题</Label>
          <Input id="studio-title" defaultValue={selected.title} readOnly />
          <Label htmlFor="studio-summary">公开介绍</Label>
          <Textarea
            id="studio-summary"
            defaultValue={selected.summary}
            readOnly
            rows={6}
          />
          <Button type="button" disabled>
            保存草稿
          </Button>
        </form>
      </aside>
    </div>
  );
}
