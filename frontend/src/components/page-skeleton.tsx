type SkeletonVariant = "gallery" | "table" | "dashboard" | "editor" | "article";
const GALLERY_HEIGHTS = [210, 270, 235, 250, 200, 285];

/** 只表达加载布局，不展示假数据；服务端流式加载和客户端请求共用同一组骨架。 */
export function PageSkeleton({
  variant = "gallery",
  heading = true,
  label = "正在加载页面",
}: {
  variant?: SkeletonVariant;
  heading?: boolean;
  label?: string;
}) {
  const lines = (count: number) =>
    Array.from({ length: count }, (_, i) => (
      <span key={i} className={`skeleton-block skeleton-line line-${i % 3}`} />
    ));
  return (
    <div
      className={`page-skeleton skeleton-${variant}`}
      role="status"
      aria-busy="true"
      aria-label={label}
    >
      <span className="sr-only">{label}</span>
      <div aria-hidden="true">
        {heading && (
          <div className="skeleton-heading">
            <span className="skeleton-block skeleton-kicker" />
            <span className="skeleton-block skeleton-title" />
            <span className="skeleton-block skeleton-subtitle" />
          </div>
        )}
        {variant === "gallery" && (
          <div className="skeleton-gallery">
            {GALLERY_HEIGHTS.map((height, i) => (
              <div key={i}>
                <div
                  className="skeleton-block skeleton-artwork"
                  style={{ height }}
                />
                {lines(2)}
              </div>
            ))}
          </div>
        )}
        {variant === "table" && (
          <div className="skeleton-table">
            {heading && (
              <div className="skeleton-table-toolbar">
                <span className="skeleton-block" />
                <span className="skeleton-block" />
              </div>
            )}
            {Array.from({ length: 6 }, (_, i) => (
              <div className="skeleton-row" key={i}>
                <span className="skeleton-block skeleton-avatar" />
                {lines(3)}
              </div>
            ))}
          </div>
        )}
        {variant === "dashboard" && (
          <>
            <div className="skeleton-stats">
              {Array.from({ length: 4 }, (_, i) => (
                <div key={i}>{lines(3)}</div>
              ))}
            </div>
            <div className="skeleton-dashboard-body">
              <div className="skeleton-block skeleton-chart" />
              <div className="skeleton-feed">{lines(7)}</div>
            </div>
          </>
        )}
        {variant === "editor" && (
          <div className="skeleton-editor-grid">
            <div className="skeleton-paper">
              <span className="skeleton-block skeleton-title" />
              <div className="skeleton-tools">
                {Array.from({ length: 9 }, (_, i) => (
                  <span key={i} className="skeleton-block" />
                ))}
              </div>
              {lines(8)}
            </div>
            <div className="skeleton-settings">{lines(10)}</div>
          </div>
        )}
        {variant === "article" && (
          <>
            <div className="skeleton-block skeleton-article-cover" />
            <div className="skeleton-editor-grid">
              <div className="skeleton-paper">{lines(8)}</div>
              <div className="skeleton-settings">{lines(5)}</div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
