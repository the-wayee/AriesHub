"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { adminRequest, type AdminPublicationSummary } from "@/lib/admin";

export function AdminPublicationList() {
  const [publications, setPublications] = useState<
    AdminPublicationSummary[] | null
  >();
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void adminRequest<AdminPublicationSummary[]>("/publications").then(
      (result) => {
        if (!active) return;
        if (result.ok) setPublications(result.data);
        else {
          setPublications(null);
          setError(
            result.status === 401 || result.status === 403
              ? "当前账号没有内容管理权限"
              : result.error.msg,
          );
        }
      },
    );
    return () => {
      active = false;
    };
  }, []);

  if (publications === undefined)
    return <p className="admin-loading">正在读取内容…</p>;
  if (publications === null) {
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
        <p>共 {publications.length} 个内容</p>
        <Link className="primary-link" href="/admin/publications/new">
          新建内容 <span aria-hidden="true">＋</span>
        </Link>
      </div>
      <div
        className="admin-publication-table"
        role="table"
        aria-label="内容列表"
      >
        <div
          className="admin-publication-row admin-publication-head"
          role="row"
        >
          <span>内容</span>
          <span>分类</span>
          <span>状态</span>
          <span>更新时间</span>
          <span />
        </div>
        {publications.map((item) => (
          <div className="admin-publication-row" role="row" key={item.id}>
            <div>
              <strong>{item.title}</strong>
              <small>/{item.slug}</small>
            </div>
            <span>{item.categoryName}</span>
            <span
              className={`publication-status status-${item.status.toLowerCase()}`}
            >
              {statusLabel(item.status)}
            </span>
            <span>
              {new Intl.DateTimeFormat("zh-CN").format(
                new Date(item.updatedAt),
              )}
            </span>
            <Link href={`/admin/publications/${item.id}`}>编辑 ↗</Link>
          </div>
        ))}
      </div>
      {publications.length === 0 && (
        <p className="admin-empty">还没有内容，从第一篇草稿开始。</p>
      )}
    </>
  );
}

function statusLabel(status: AdminPublicationSummary["status"]) {
  return status === "PUBLISHED"
    ? "已发布"
    : status === "ARCHIVED"
      ? "已下架"
      : "草稿";
}
