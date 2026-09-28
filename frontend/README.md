# AriesHub 前端

Next.js 16.3.6 + React 19.2.8 + TypeScript + App Router + Tailwind CSS 4，使用 npm 管理依赖。

## 启动

从本目录运行 `npm ci`、`npm run dev`，访问 http://localhost:3000。后端与数据库启动方式见 [仓库说明](../README.md)。本次验证 Node.js 26.5.0 / npm 11.17.0，团队建议使用 Node.js 24 或更高版本。

页面已连接真实 Java API；没有后端时显示内容服务暂不可用，不返回模拟成功数据。

## 页面

- `/`：社区落地页，从数据库获取最近发布的三个案例，并说明使用流程。
- `/cases`：关键词、分类、免费或付费筛选，分页和空结果提示。
- `/cases/[slug]`：公开预览、适用条件和交付说明；免费案例显示完整正文，付费案例显示尚未开放。
- `/register`、`/login`：先通过 Resend 获取六位邮箱验证码，再调用 Java 身份接口；成功后由 Sa-Token 写入 HttpOnly Cookie。
- `/account`：显示当前账号，支持退出登录，并预留收藏、购买与学习记录入口。
- `/admin`：管理员案例列表；支持进入新建与编辑页，保存草稿、发布和下架。
- 不存在或未公开案例显示 404 页面；提供加载态与错误重试。

## 脚本

| 命令 | 作用 |
| --- | --- |
| `npm run dev` | 开发服务器与热更新 |
| `npm run generate:api` | 从 `doc/openapi.json` 生成并格式化接口类型 |
| `npm run format` / `format:check` | 格式化 / 检查源文件格式 |
| `npm run lint` | ESLint 检查 |
| `npm run typecheck` | 生成路由类型并检查 TypeScript |
| `npm run build` | 生产构建，构建时不依赖 Java 服务在线 |
| `npm start` | 运行生产构建 |
| `npm run test:e2e` | 桌面与手机浏览器流程检查 |

类型检查与构建按顺序执行，避免同时写入 `.next`。接口调整时先修改 OpenAPI，再重新生成 `api-schema.d.ts`，不要手工修改生成文件。

## 与 Java 的边界

- `BACKEND_ORIGIN` 是可信 Java 服务的 origin，默认 `http://127.0.0.1:8080`。修改时复制 `.env.example` 为 `.env.local`。
- `src/lib/catalog.ts` 仅在服务端使用，统一超时和错误返回，显式 `no-store` 避免下架后继续使用缓存。
- 页面通过服务端组件读取 Java；浏览器直接调用 API 时用相对 `/api/v1/...`，开发 rewrites 负责转发。
- 生产同域 API 代理交给部署网关；`npm start` 下服务端页面仍通过 `BACKEND_ORIGIN` 读取 Java。
- 前端不访问数据库，不读取或自行签发 token。Sa-Token 登录态由 Java 判断，浏览器只自动携带同域 HttpOnly Cookie。
- `react-markdown` 禁止原始 HTML，保留安全 URL 协议处理，并将远程图片显示为文字占位。

## 测试

启动真实 dev 后端后执行：

```bash
npx playwright install chromium
npm run test:e2e
```

测试默认复用或启动 3000 端口前端，已有前端使用其他端口时设置 `E2E_PORT`。使用已有 Chromium 时可设置 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` 指向可执行文件；不在仓库中写死本机路径。案例浏览连接真实 Java，身份和后台浏览器测试拦截 API，以免测试向真实邮箱发送验证码；Redis、验证码消费和管理员权限由 Java 集成测试覆盖。测试生成目录已加入 Git 忽略。

## 目录

- `src/app`：首页、案例、身份、账号、内容后台、加载和错误页面。
- `src/components`：站点外壳、身份表单、账号面板、后台编辑器、案例卡片、Markdown、统一提示。
- `src/lib`：API 类型、服务端案例请求、浏览器身份与后台请求和筛选条件处理。
- `e2e`：真实后端驱动的浏览器流程测试。
