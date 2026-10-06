"use client";
import { Dialog } from "@base-ui/react/dialog";
import { useEffect, useState, type FormEvent } from "react";
import { Search, RefreshCw, Users } from "lucide-react";
import { adminRequest } from "@/lib/admin";
import type { Member, Page } from "@/lib/operations";
import { PageSkeleton } from "./page-skeleton";
export function AdminMembers() {
  const [data, setData] = useState<Page<Member>>();
  const [query, setQuery] = useState("");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(1);
  const [error, setError] = useState("");
  const [pending, setPending] = useState("");
  const [refresh, setRefresh] = useState(0);
  const [target, setTarget] = useState<Member>();
  useEffect(() => {
    let active = true;
    void adminRequest<Page<Member>>(
      `/users?q=${encodeURIComponent(query)}&status=${status}&page=${page}`,
    ).then((r) => {
      if (!active) return;
      if (r.ok) {
        setData(r.data);
        setError("");
      } else setError(r.error.msg);
    });
    return () => {
      active = false;
    };
  }, [query, status, page, refresh]);
  async function change() {
    if (!target) return;
    setPending(target.id);
    const r = await adminRequest<Member>(`/users/${target.id}/status`, {
      method: "PUT",
      body: JSON.stringify({
        status: target.status === "ACTIVE" ? "DISABLED" : "ACTIVE",
      }),
    });
    setPending("");
    if (r.ok) {
      setTarget(undefined);
      setRefresh(refresh + 1);
    } else setError(r.error.msg);
  }
  return (
    <div className="ops-page">
      <div className="ops-page-heading">
        <div>
          <p className="ops-kicker">COMMUNITY / MEMBERS</p>
          <h1>成员管理</h1>
          <p>了解成员，管理账号状态，保护社区交流。</p>
        </div>
        <span className="ops-pill">
          <Users size={15} />共 {data?.total ?? "—"} 位成员
        </span>
      </div>
      <section className="ops-panel ops-table-panel">
        <div className="ops-table-tools">
          <form
            className="ops-search"
            onSubmit={(e: FormEvent) => {
              e.preventDefault();
              setQuery(search);
              setPage(1);
            }}
          >
            <Search size={17} />
            <input
              aria-label="搜索成员"
              placeholder="搜索昵称或邮箱"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <button type="submit">搜索</button>
          </form>
          <select
            aria-label="账号状态筛选"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(1);
            }}
          >
            <option value="">全部状态</option>
            <option value="ACTIVE">正常</option>
            <option value="DISABLED">已停用</option>
          </select>
          <button
            className="ops-icon-button"
            aria-label="刷新成员"
            onClick={() => setRefresh(refresh + 1)}
          >
            <RefreshCw size={17} />
          </button>
        </div>
        {error && !target && (
          <p className="ops-error" role="alert">
            {error}
          </p>
        )}
        <div className="ops-table-scroll">
          <table className="ops-table">
            <thead>
              <tr>
                <th>成员</th>
                <th>身份</th>
                <th>状态</th>
                <th>可用积分</th>
                <th>加入时间</th>
                <th>最近登录</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {data?.items.map((m) => (
                <tr key={m.id}>
                  <td>
                    <div className="ops-member-cell">
                      <span>{m.nickname.slice(0, 1)}</span>
                      <div>
                        <strong>{m.nickname}</strong>
                        <small>{m.email}</small>
                      </div>
                    </div>
                  </td>
                  <td>{m.role === "ADMIN" ? "管理员" : "成员"}</td>
                  <td>
                    <span className="ops-status">
                      {m.status === "ACTIVE" ? "正常" : "已停用"}
                    </span>
                  </td>
                  <td>{m.creditBalance.toLocaleString()}</td>
                  <td>{new Date(m.createdAt).toLocaleDateString("zh-CN")}</td>
                  <td>
                    {m.lastLoginAt
                      ? new Date(m.lastLoginAt).toLocaleDateString("zh-CN")
                      : "尚未登录"}
                  </td>
                  <td>
                    {m.role === "ADMIN" ? (
                      <small>受保护账号</small>
                    ) : (
                      <button
                        className="ops-text-button"
                        disabled={pending === m.id}
                        onClick={() => setTarget(m)}
                      >
                        {m.status === "ACTIVE" ? "停用" : "恢复"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {data?.items.length === 0 && (
          <div className="ops-empty">没有找到匹配的成员，试试其他关键词。</div>
        )}
        {!data && !error && (
          <PageSkeleton variant="table" heading={false} label="正在读取成员" />
        )}
        <div className="ops-pagination">
          <span>
            第 {page} 页 · 共 {data?.total ?? 0} 位成员
          </span>
          <button disabled={page === 1} onClick={() => setPage(page - 1)}>
            上一页
          </button>
          <button
            disabled={!data || page * data.size >= data.total}
            onClick={() => setPage(page + 1)}
          >
            下一页
          </button>
        </div>
      </section>
      <Dialog.Root
        open={!!target}
        onOpenChange={(open) => {
          if (!open && !pending) setTarget(undefined);
        }}
      >
        <Dialog.Portal>
          <Dialog.Backdrop className="ops-dialog-backdrop" />
          {target && (
            <Dialog.Popup className="ops-dialog">
              {error && (
                <p className="ops-error" role="alert">
                  {error}
                </p>
              )}
              <Dialog.Title>
                {target.status === "ACTIVE" ? "停用" : "恢复"} {target.nickname}{" "}
                的账号？
              </Dialog.Title>
              <Dialog.Description>
                {target.status === "ACTIVE"
                  ? "停用后，该成员将无法登录和访问成员功能。已有内容与积分记录会保留。"
                  : "恢复后，该成员可以重新登录社区。"}
              </Dialog.Description>
              <div>
                <button
                  onClick={() => setTarget(undefined)}
                  disabled={!!pending}
                >
                  取消
                </button>
                <button
                  className="ops-primary"
                  disabled={!!pending}
                  onClick={() => void change()}
                >
                  {pending ? "处理中…" : "确认"}
                </button>
              </div>
            </Dialog.Popup>
          )}
        </Dialog.Portal>
      </Dialog.Root>
    </div>
  );
}
