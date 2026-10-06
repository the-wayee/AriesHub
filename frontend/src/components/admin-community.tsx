"use client";
import { useEffect, useState } from "react";
import { adminRequest } from "@/lib/admin";
import type { CommentRow, LedgerRow, Page } from "@/lib/operations";
export function AdminCommunity({ ledger = false }: { ledger?: boolean }) {
  const [data, setData] = useState<Page<CommentRow> | Page<LedgerRow>>();
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState("");
  const [refresh, setRefresh] = useState(0);
  const [error, setError] = useState("");
  const [pending, setPending] = useState("");
  useEffect(() => {
    let active = true;
    void adminRequest<Page<CommentRow> | Page<LedgerRow>>(
      ledger
        ? `/operations/ledger?page=${page}`
        : `/discussions/comments?page=${page}&status=${status}`,
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
  }, [ledger, page, status, refresh]);
  async function action(c: CommentRow, hide: boolean) {
    setPending(c.id);
    const r = await adminRequest<void>(
      hide ? `/discussions/comments/${c.id}/hide` : "/discussions/threads/lock",
      {
        method: "POST",
        ...(hide
          ? {}
          : {
              body: JSON.stringify({
                targetType: c.targetType,
                targetKey: c.targetKey,
              }),
            }),
      },
    );
    setPending("");
    if (r.ok) setRefresh(refresh + 1);
    else setError(r.error.msg);
  }
  return (
    <div className="ops-page">
      <div className="ops-page-heading">
        <div>
          <p className="ops-kicker">
            COMMUNITY / {ledger ? "CREDITS" : "MODERATION"}
          </p>
          <h1>{ledger ? "积分与权益" : "评论与审核"}</h1>
          <p>
            {ledger
              ? "按记录查看积分变化，保持每一次消耗与发放可追溯。"
              : "查看成员讨论，隐藏违规评论，必要时锁定讨论。"}
          </p>
        </div>
      </div>
      {ledger && (
        <p className="ops-notice">
          这里展示真实积分流水。积分发放、内容解锁与充值支付的完整操作流程尚未接入，本页不直接修改余额。
        </p>
      )}
      <section className="ops-panel ops-table-panel">
        <div className="ops-table-tools">
          <h2>{ledger ? "积分流水" : "社区评论"}</h2>
          {!ledger && (
            <select
              aria-label="评论状态筛选"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(1);
              }}
            >
              <option value="">全部状态</option>
              <option value="PUBLISHED">公开可见</option>
              <option value="HIDDEN">已隐藏</option>
              <option value="DELETED">作者已删除</option>
            </select>
          )}
          <button onClick={() => setRefresh(refresh + 1)}>刷新</button>
        </div>
        {error && (
          <p className="ops-error" role="alert">
            {error}
          </p>
        )}
        <div className="ops-table-scroll">
          {ledger ? (
            <table className="ops-table">
              <thead>
                <tr>
                  <th>成员</th>
                  <th>类型</th>
                  <th>变动积分</th>
                  <th>变动后余额</th>
                  <th>备注</th>
                  <th>时间</th>
                </tr>
              </thead>
              <tbody>
                {(data as Page<LedgerRow>)?.items.map((l) => (
                  <tr key={l.id}>
                    <td>
                      <strong>{l.nickname}</strong>
                      <small>{l.email}</small>
                    </td>
                    <td>
                      {{
                        GRANT: "发放",
                        SPEND: "消费",
                        TOP_UP: "充值",
                        REFUND: "退款",
                        ADJUSTMENT: "调整",
                        EXPIRE: "过期",
                      }[l.reason] ?? l.reason}
                    </td>
                    <td>
                      {l.delta > 0 ? "+" : ""}
                      {l.delta}
                    </td>
                    <td>{l.balanceAfter}</td>
                    <td>{l.note ?? "—"}</td>
                    <td>{new Date(l.createdAt).toLocaleString("zh-CN")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <table className="ops-table ops-comments-table">
              <thead>
                <tr>
                  <th>评论与作者</th>
                  <th>所属内容</th>
                  <th>状态</th>
                  <th>发布时间</th>
                  <th>审核操作</th>
                </tr>
              </thead>
              <tbody>
                {(data as Page<CommentRow>)?.items.map((c) => (
                  <tr key={c.id}>
                    <td>
                      <strong>{c.authorName}</strong>
                      <p>
                        {c.status === "DELETED" ? "作者已删除正文" : c.body}
                      </p>
                    </td>
                    <td>
                      {c.title}
                      <small>
                        {c.threadStatus === "LOCKED"
                          ? "讨论已锁定"
                          : "讨论开放"}
                      </small>
                    </td>
                    <td>
                      <span className="ops-status">
                        {{
                          PUBLISHED: "公开可见",
                          HIDDEN: "已隐藏",
                          DELETED: "已删除",
                        }[c.status] ?? c.status}
                      </span>
                    </td>
                    <td>{new Date(c.createdAt).toLocaleDateString("zh-CN")}</td>
                    <td>
                      <div className="ops-row-actions">
                        <button
                          disabled={!!pending || c.status !== "PUBLISHED"}
                          onClick={() => void action(c, true)}
                        >
                          隐藏评论
                        </button>
                        <button
                          disabled={!!pending || c.threadStatus === "LOCKED"}
                          onClick={() => void action(c, false)}
                        >
                          锁定讨论
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        {data?.items.length === 0 && (
          <div className="ops-empty">
            {ledger
              ? "还没有积分流水。有实际的积分变动后，记录会出现在这里。"
              : "当前筛选下没有评论。"}
          </div>
        )}
        {!data && !error && <p className="ops-loading">正在读取记录…</p>}
        <div className="ops-pagination">
          <span>
            共 {data?.total ?? 0} 条 · 第 {page} 页
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
    </div>
  );
}
