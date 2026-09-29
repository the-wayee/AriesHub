import type { Metadata } from "next";
import Link from "next/link";
import { CaseCard } from "@/components/case-card";
import { ContentState } from "@/components/content-state";
import { getCases, getCategories } from "@/lib/catalog";
import { filtersQuery, parseFilters } from "@/lib/catalog-filters";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export const metadata: Metadata = { title: "案例库" };

export default async function CasesPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const filters = parseFilters(await searchParams);
  if (!filters)
    return (
      <ContentState
        title="筛选条件不正确"
        message="请使用有效的关键词、分类和页码，或清除条件重新浏览。"
        label="清除筛选"
      />
    );
  const [cases, categories] = await Promise.all([
    getCases(filtersQuery(filters)),
    getCategories(),
  ]);
  if (!cases.ok || !categories.ok) {
    const failure = !cases.ok ? cases : !categories.ok ? categories : null;
    return (
      <ContentState
        title="案例暂时加载不了"
        message="内容服务暂时不可用，请稍后再试。"
        href={`/cases?${filtersQuery(filters)}`}
        reload
        label="重新加载"
        requestId={failure?.requestId}
      />
    );
  }
  const data = cases.data;
  return (
    <div className="library-page">
      <header className="page-heading">
        <p className="eyebrow">THE CASE LIBRARY</p>
        <h1>把好奇心，落在实践里。</h1>
        <p>找一个具体问题，从完整的过程里获得启发。</p>
      </header>
      <form action="/cases" className="catalog-filters">
        <div className="search-field">
          <Label htmlFor="q">关键词</Label>
          <Input
            id="q"
            name="q"
            type="search"
            maxLength={120}
            defaultValue={filters.q}
            placeholder="搜索标题或简介…"
          />
        </div>
        <div>
          <Label htmlFor="category">内容方向</Label>
          <Select
            name="category"
            defaultValue={filters.category}
            items={[
              { value: "", label: "全部方向" },
              ...categories.data.map((category) => ({
                value: category.slug,
                label: `${category.name}（${category.caseCount}）`,
              })),
              ...(filters.category &&
              !categories.data.some(
                (category) => category.slug === filters.category,
              )
                ? [{ value: filters.category, label: "未找到该分类" }]
                : []),
            ]}
          >
            <SelectTrigger id="category" aria-label="内容方向">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">全部方向</SelectItem>
              {categories.data.map((category) => (
                <SelectItem key={category.id} value={category.slug}>
                  {category.name}（{category.caseCount}）
                </SelectItem>
              ))}
              {filters.category &&
                !categories.data.some((c) => c.slug === filters.category) && (
                  <SelectItem value={filters.category}>未找到该分类</SelectItem>
                )}
            </SelectContent>
          </Select>
        </div>
        <div>
          <Label htmlFor="access">阅读方式</Label>
          <Select
            name="access"
            defaultValue={filters.access}
            items={[
              { value: "", label: "全部案例" },
              { value: "FREE", label: "免费阅读" },
              { value: "PAID", label: "付费预览" },
            ]}
          >
            <SelectTrigger id="access" aria-label="阅读方式">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">全部案例</SelectItem>
              <SelectItem value="FREE">免费阅读</SelectItem>
              <SelectItem value="PAID">付费预览</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <Input type="hidden" name="size" value={filters.size} />
        <Button className="primary-link" type="submit">
          筛选案例 ↗
        </Button>
      </form>
      <div className="results-heading">
        <p>共 {data.total} 个案例</p>
        {(filters.q || filters.category || filters.access) && (
          <Link className="text-link" href="/cases">
            清除筛选
          </Link>
        )}
      </div>
      {data.items.length ? (
        <div className="direction-grid">
          {data.items.map((item, index) => (
            <CaseCard
              key={item.id}
              item={item}
              index={(data.page - 1) * data.size + index}
            />
          ))}
        </div>
      ) : (
        <ContentState
          title={data.total ? "这一页没有案例" : "暂时没有符合条件的案例"}
          message="试试其他关键词或分类，也可以返回查看全部内容。"
          label="查看全部案例"
        />
      )}
      {data.totalPages > 0 && (
        <nav className="pagination" aria-label="案例分页">
          {data.page > 1 && (
            <Link
              href={`/cases?${filtersQuery(filters, Math.min(data.page - 1, data.totalPages))}`}
            >
              上一页
            </Link>
          )}
          <span aria-current="page">
            第 {data.page} 页 / 共 {data.totalPages} 页
          </span>
          {data.page < data.totalPages && (
            <Link href={`/cases?${filtersQuery(filters, data.page + 1)}`}>
              下一页
            </Link>
          )}
        </nav>
      )}
      <p className="library-note">
        标注「演示案例」的内容用于体验浏览流程，演示价格不构成售卖报价。
      </p>
    </div>
  );
}
