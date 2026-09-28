import ReactMarkdown from "react-markdown";

export function Markdown({ children }: { children: string }) {
  return (
    <div className="prose">
      <ReactMarkdown
        skipHtml
        components={{
          // 禁止正文加载远程追踪图片或执行原始 HTML；链接仍由 Markdown 渲染器校验协议。
          img: ({ alt }) => (
            <span className="image-placeholder">
              [图片：{alt || "未提供描述"}]
            </span>
          ),
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  );
}
