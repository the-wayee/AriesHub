"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { adminRequest, type AdminCaseSummary } from "@/lib/admin";

export function AdminCaseList() {
  const [cases, setCases] = useState<AdminCaseSummary[] | null>();
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void adminRequest<AdminCaseSummary[]>("/cases").then((result) => {
      if (!active) return;
      if (result.ok) setCases(result.data);
      else {
        setCases(null);
        setError(
          result.status === 401 || result.status === 403
            ? "当前账号没有内容管理权限"
            : result.error.message,
        );
      }
    });
    return () => {
      active = false;
    };
  }, []);

  if (cases === undefined)
    return <p className="admin-loading">正在读取案例…</p>;
  if (cases === null) {
    return (
      <div className="admin-empty">
        <h2>{error}</h2>
        <p>请使用配置在 ADMIN_EMAILS 中的管理员邮箱注册或登录。</p>
        <Link className="primary-link" href="/login">
          前往登录 ↗
        </Link>
      </div>
    );
  }
  return (
    <>
      <div className="admin-toolbar">
        <p>共 {cases.length} 个案例</p>
        <Link className="primary-link" href="/admin/cases/new">
          新建案例 <span aria-hidden="true">＋</span>
        </Link>
      </div>
      <div className="admin-case-table" role="table" aria-label="案例列表">
        <div className="admin-case-row admin-case-head" role="row">
          <span>案例</span>
          <span>分类</span>
          <span>状态</span>
          <span>更新时间</span>
          <span />
        </div>
        {cases.map((item) => (
          <div className="admin-case-row" role="row" key={item.id}>
            <div>
              <strong>{item.title}</strong>
              <small>/{item.slug}</small>
            </div>
            <span>{item.categoryName}</span>
            <span className={`case-status status-${item.status.toLowerCase()}`}>
              {statusLabel(item.status)}
            </span>
            <span>
              {new Intl.DateTimeFormat("zh-CN").format(
                new Date(item.updatedAt),
              )}
            </span>
            <Link href={`/admin/cases/${item.id}`}>编辑 ↗</Link>
          </div>
        ))}
      </div>
      {cases.length === 0 && (
        <p className="admin-empty">还没有案例，从第一篇草稿开始。</p>
      )}
    </>
  );
}

function statusLabel(status: AdminCaseSummary["status"]) {
  return status === "PUBLISHED"
    ? "已发布"
    : status === "ARCHIVED"
      ? "已下架"
      : "草稿";
}
