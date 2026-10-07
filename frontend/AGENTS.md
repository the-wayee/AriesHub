<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

## 项目交互规范

- 修改前端页面和组件前阅读 `../doc/FRONTEND_DESIGN_LANGUAGE.md`，遵循其中「全站交互手感与动效细节」。点击、悬浮、选中、展开、加载及完成状态都需要细腻、连贯的反馈；刷新和筛选保持布局稳定。
- 社区动态不提供刷新按钮，切换 Tab 只播放列表区域动效。加载、完成与新内容交叉衔接，局部加载不加装饰性文字，不照搬其他产品的图形。
