"use client";
import { PageSkeleton } from "./page-skeleton";
import { PublicationMedia } from "./publication-media";
import Link from "next/link";
import { useEffect, useState, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import {
  Search,
  Plus,
  FileText,
  ArrowUpRight,
  Star,
  BookOpen,
  Layers,
  GraduationCap,
} from "lucide-react";
import { adminRequest, type AdminPublicationSummary } from "@/lib/admin";
const states: Record<string, string> = {
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  ARCHIVED: "已下架",
};
const types: Record<string, string> = {
  CASE_STUDY: "实战案例",
  ARTICLE: "学习文章",
  COURSE: "课程",
};
/** 类型保留线性图标，状态由色点表达，避免每个字段都变成带框标签。 */
const typeIcons = {
  CASE_STUDY: Layers,
  ARTICLE: BookOpen,
  COURSE: GraduationCap,
};
function ContentType({
  type,
}: {
  type: AdminPublicationSummary["publicationType"];
}) {
  const Icon = typeIcons[type];
  return (
    <span className="ops-content-type">
      <Icon size={15} aria-hidden="true" />
      {types[type]}
    </span>
  );
}
export function AdminPublicationList() {
  return (
    <Suspense fallback={<PageSkeleton variant="table" label="正在读取内容" />}>
      <ContentList />
    </Suspense>
  );
}
function ContentList() {
  const params = useSearchParams();
  const [items, setItems] = useState<AdminPublicationSummary[]>();
  const [error, setError] = useState("");
  const [q, setQ] = useState("");
  const [status, setStatus] = useState(params.get("status") ?? "");
  const [type, setType] = useState("");
  const [access, setAccess] = useState("");
  const [page, setPage] = useState(1);
  const [pending, setPending] = useState("");
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    void adminRequest<AdminPublicationSummary[]>("/publications").then((r) => {
      if (!active) return;
      if (r.ok) {
        setItems(r.data);
        setError("");
      } else setError(r.error.msg);
    });
    return () => {
      active = false;
    };
  }, [attempt]);
  async function action(id: string, operation: "publish" | "archive") {
    setPending(id);
    const r = await adminRequest<AdminPublicationSummary>(
      `/publications/${id}/${operation}`,
      { method: "POST" },
    );
    setPending("");
    if (r.ok) setAttempt(attempt + 1);
    else setError(r.error.msg);
  }
  const filtered = items?.filter(
    (x) =>
      (!status || x.status === status) &&
      (!type || x.publicationType === type) &&
      (!access || x.accessType === access) &&
      (!q ||
        x.title.toLowerCase().includes(q.toLowerCase()) ||
        x.id.includes(q)),
  );
  return (
    <div className="ops-page">
      <div className="ops-page-heading">
        <div>
          <p className="ops-kicker">STUDIO / PUBLICATIONS</p>
          <h1>内容管理</h1>
          <p>把实践写成案例，让经验成为学习文章与课程。</p>
        </div>
        <Link className="ops-primary" href="/admin/publications/new">
          <Plus size={17} />
          新建内容
        </Link>
      </div>
      <div className="ops-content-counts">
        <button
          onClick={() => {
            setStatus("");
            setPage(1);
          }}
        >
          全部内容 <b>{items?.length ?? "—"}</b>
        </button>
        {Object.entries(states).map(([value, label]) => (
          <button
            key={value}
            onClick={() => {
              setStatus(value);
              setPage(1);
            }}
            aria-pressed={status === value}
          >
            {label}{" "}
            <b>{items?.filter((x) => x.status === value).length ?? "—"}</b>
          </button>
        ))}
      </div>
      <section className="ops-panel ops-table-panel">
        <div className="ops-table-tools">
          <div className="ops-search">
            <Search size={17} />
            <input
              aria-label="搜索内容"
              value={q}
              placeholder="搜索标题或页面地址"
              onChange={(e) => {
                setQ(e.target.value);
                setPage(1);
              }}
            />
          </div>
          <select
            aria-label="内容类型筛选"
            value={type}
            onChange={(e) => {
              setType(e.target.value);
              setPage(1);
            }}
          >
            <option value="">全部类型</option>
            {Object.entries(types).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </select>
          <select
            aria-label="阅读方式筛选"
            value={access}
            onChange={(e) => {
              setAccess(e.target.value);
              setPage(1);
            }}
          >
            <option value="">免费与积分</option>
            <option value="FREE">免费</option>
            <option value="CREDIT">积分内容</option>
          </select>
        </div>
        {error && (
          <p className="ops-error" role="alert">
            {error}
            <button onClick={() => setAttempt(attempt + 1)}>重试</button>
          </p>
        )}
        <div className="ops-table-scroll">
          <table className="ops-table ops-content-table" aria-label="内容列表">
            <thead>
              <tr>
                <th>内容</th>
                <th>类型 / 分类</th>
                <th>阅读方式</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {filtered?.slice((page - 1) * 12, page * 12).map((x) => (
                <tr key={x.id}>
                  <td>
                    <div className="ops-publication-cell">
                      <span className="ops-content-icon">
                        {x.coverFileId && x.cover?.url ? (
                          <PublicationMedia
                            id={x.coverFileId}
                            signedUrl={x.cover.url}
                            preview
                            admin
                            label={`${x.title}封面`}
                          />
                        ) : (
                          <FileText size={22} />
                        )}
                      </span>
                      <div>
                        <Link href={`/admin/publications/${x.id}`}>
                          <strong>{x.title}</strong>
                          {x.featured && <Star size={12} fill="currentColor" />}
                        </Link>
                        <small>/publications/{x.id}</small>
                      </div>
                    </div>
                  </td>
                  <td>
                    <ContentType type={x.publicationType} />
                    <small className="ops-category-label">
                      <i
                        aria-hidden="true"
                        style={{
                          backgroundColor: x.categoryColor ?? "#4967a9",
                        }}
                      />
                      {x.categoryName}
                    </small>
                  </td>
                  <td>
                    {x.accessType === "FREE" ? (
                      <span className="ops-access-free">免费</span>
                    ) : (
                      <span className="ops-access-credit">
                        <b>{x.creditPrice.toLocaleString()}</b>
                        <span>积分</span>
                      </span>
                    )}
                  </td>
                  <td>
                    <span
                      className="ops-publication-status"
                      data-status={x.status}
                    >
                      <i aria-hidden="true" />
                      {states[x.status]}
                    </span>
                  </td>
                  <td>{new Date(x.updatedAt).toLocaleDateString("zh-CN")}</td>
                  <td>
                    <div className="ops-row-actions">
                      <Link href={`/admin/publications/${x.id}`}>
                        编辑 <ArrowUpRight size={13} />
                      </Link>
                      <button
                        disabled={!!pending}
                        onClick={() =>
                          void action(
                            x.id,
                            x.status === "PUBLISHED" ? "archive" : "publish",
                          )
                        }
                      >
                        {pending === x.id
                          ? "处理中…"
                          : x.status === "PUBLISHED"
                            ? "下架"
                            : "发布"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!items && !error && (
          <PageSkeleton variant="table" heading={false} label="正在读取内容" />
        )}
        {filtered?.length === 0 && (
          <div className="ops-empty">
            <FileText size={30} />
            <h2>
              {items?.length
                ? "没有找到匹配的内容"
                : "第一篇好内容，从这里开始"}
            </h2>
            <p>试试其他筛选条件，或创建一篇新的内容。</p>
            <Link href="/admin/publications/new">开始创作 →</Link>
          </div>
        )}
        <div className="ops-pagination">
          <span>
            共 {filtered?.length ?? 0} 篇 · 第 {page} 页
          </span>
          <button disabled={page === 1} onClick={() => setPage(page - 1)}>
            上一页
          </button>
          <button
            disabled={!filtered || page * 12 >= filtered.length}
            onClick={() => setPage(page + 1)}
          >
            下一页
          </button>
        </div>
      </section>
    </div>
  );
}
