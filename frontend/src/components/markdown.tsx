"use client";
import ReactMarkdown, { defaultUrlTransform } from "react-markdown";
import remarkGfm from "remark-gfm";
import { PublicationMedia } from "./publication-media";
/** 正文支持表格与任务清单；仍禁用原始 HTML，素材经业务接口读取。 */
export function Markdown({
  children,
  admin = false,
  publicationId,
}: {
  children: string;
  admin?: boolean;
  publicationId?: string;
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
                publicationId={publicationId}
              />
            ) : (
              <span className="image-placeholder">
                [图片：{alt || "未提供描述"}]
              </span>
            ),
          a: ({ href, children }) =>
            href?.startsWith("media:") ? (
              <PublicationMedia
                id={href.slice(6)}
                label={String(children)}
                kind="ATTACHMENT"
                admin={admin}
                publicationId={publicationId}
              />
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
