"use client";
import ReactMarkdown, { defaultUrlTransform } from "react-markdown";
import remarkGfm from "remark-gfm";
import { PublicationMedia } from "./publication-media";
/** 正文支持表格与任务清单；仍禁用原始 HTML，素材经业务接口读取。 */
export function Markdown({
  children,
  admin = false,
  publicationId,
  attachmentsLocked = false,
  shareToken,
}: {
  children: string;
  admin?: boolean;
  publicationId?: string;
  attachmentsLocked?: boolean;
  shareToken?: string;
}) {
  return (
    <div className="prose">
      <ReactMarkdown
        skipHtml
        remarkPlugins={[remarkGfm]}
        urlTransform={(url) =>
          url.startsWith("media:") ? url : defaultUrlTransform(url)
        }
        components={{
          img: ({ src, alt }) =>
            typeof src === "string" && src.startsWith("media:") ? (
              <PublicationMedia
                id={src.slice(6)}
                label={alt ?? "文章图片"}
                kind={alt?.startsWith("视频：") ? "VIDEO" : "IMAGE"}
                admin={admin}
                shareToken={shareToken}
                publicationId={publicationId}
                preview
              />
            ) : (
              <span className="image-placeholder">
                [图片：{alt || "未提供描述"}]
              </span>
            ),
          a: ({ href, children }) =>
            href?.startsWith("media:") ? (
              attachmentsLocked ? (
                <span className="publication-attachment">
                  {children} · 解锁文章后可下载
                </span>
              ) : (
                <PublicationMedia
                  id={href.slice(6)}
                  label={String(children)}
                  kind="ATTACHMENT"
                  admin={admin}
                  shareToken={shareToken}
                  publicationId={publicationId}
                />
              )
            ) : (
              <a href={href} target="_blank" rel="noopener noreferrer">
                {children}
              </a>
            ),
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  );
}
