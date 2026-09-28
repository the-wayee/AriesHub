# AriesHub 前端

Next.js 16.3.6 + React 19.2.8 + TypeScript + App Router + Tailwind CSS 4，使用 npm 管理依赖。用户页面与后续管理后台共用此工程。

## 启动

要求 Node.js 20.9+；本次使用 Node.js 26.5.0、npm 11.17.0。进入此目录后执行：

```bash
npm ci
npm run dev
```

浏览器访问 http://localhost:3000。初始页面为中文筹备首页，包含站内锚点和可展开的说明；没有模拟商品销售、登录或购买行为，也不要求后端已启动。

## 脚本

| 命令 | 作用 |
| --- | --- |
| `npm run dev` | 开发服务器与热更新 |
| `npm run lint` | ESLint 检查，警告也视为失败 |
| `npm run typecheck` | 生成 Next.js 路由类型并检查 TypeScript |
| `npm run build` | 生产构建 |
| `npm start` | 运行已生成的生产构建 |

类型检查与生产构建均会生成 `.next` 类型文件，按顺序运行，避免同时写入。

## 对接 Java

浏览器请求使用相对路径 `/api/v1/...`。开发服务器通过 `next.config.ts` 的 rewrites 转发至 `BACKEND_ORIGIN`，默认 `http://127.0.0.1:8080`。

需要更改时：

```bash
cp .env.example .env.local
```

修改 `.env.local` 后重启开发服务器。此变量仅用于服务端配置，不使用 `NEXT_PUBLIC_` 前缀。目标必须为可信 HTTP(S) origin，不包含路径或账户凭证。

目前 backend 没有业务 API，因此代理目标的业务请求尚不能成功；不要将前端筹备页视为已完成联调。

生产模式不启用开发转发，由部署反向代理将同域 `/api/v1/*` 路由到 Java。生产代理配置将在上线阶段提供；`npm start` 当前只用于验证前端页面。

后续登录使用 Java Sa-Token 签发的 HttpOnly Cookie。前端不保存 localStorage token、不直连 PostgreSQL、不实现另一套业务鉴权。Cookie 认证的写请求还需后端提供 CSRF 防护，详见 [项目文档](../doc/PROJECT.md)。

## 目录

- `src/app/layout.tsx`：中文语言、站点标题和描述。
- `src/app/page.tsx`：筹备首页；计划方向为静态介绍，不是业务数据。
- `src/app/globals.css`：颜色、字体、布局与移动端适配。
- `src/app/icon.svg`：站点图标。
- `next.config.ts`：开发 API 代理。

页面默认使用服务端组件，出现状态或事件处理需求时再加入客户端组件。当前不加载外部字体，不依赖图片远程服务。新增业务接口后先定义契约，再扩展 API 客户端和页面。
