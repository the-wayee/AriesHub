# AriesHub 前端

Next.js 16.3.6 + React 19.2.8 + TypeScript + App Router + Tailwind CSS 4，使用 npm 管理依赖。

## 启动

从本目录运行 `npm ci`、`npm run dev`，访问 http://localhost:3200。后端与数据库启动方式见 [仓库说明](../README.md)。本次验证 Node.js 26.5.0 / npm 11.17.0，团队建议使用 Node.js 24 或更高版本。

页面已连接真实 Java API；没有后端时显示内容服务暂不可用，不返回模拟成功数据。

成员页面 `/home`、`/community`、`/my-content`、`/members`、`/account`、`/learn/*`、`/checkout/*` 与后台 `/admin/*` 要求登录。Next Proxy 在无 Cookie 时重定向登录；有 Cookie 后仍由 Java `/users/me` 验证，验证前不显示成员内容，失效会话重定向，服务异常显示重试。后台再检查管理员角色；业务 API 的最终授权仍由 Java 执行。`/discover` 与公开详情保持访客可访问。注册演示完成后进入公开探索，不绕过守卫。

账号与后台沿用 Cosmos 黑白极简、画廊方向，规范与来源见 [设计语言](../doc/FRONTEND_DESIGN_LANGUAGE.md)。后台专用样式在 `src/app/admin.css`，账号与用户菜单在 `src/app/account.css`。

## 页面

- `/`：社区落地页，从数据库获取最近发布的三个案例，并说明使用流程。
- `/cases`：关键词、分类、免费或付费筛选，分页和空结果提示。
- `/cases/[slug]`：公开预览、适用条件和交付说明；免费案例显示完整正文，付费案例显示尚未开放。
- `/register`：先通过 Resend 获取六位邮箱验证码，再创建账号；`/login`：使用邮箱和密码登录，不再需要验证码。成功后由 Sa-Token 写入 HttpOnly Cookie。
- `/account`：通过右上角用户菜单进入，上传或移除头像、修改昵称和个性签名，资料保存到后端；账号设置不占用社区导航 tab。用户菜单还提供我的空间、管理员内容后台和退出登录。
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

测试默认复用或启动 3200 端口前端，已有前端使用其他端口时设置 `E2E_PORT`。使用已有 Chromium 时可设置 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` 指向可执行文件；不在仓库中写死本机路径。案例浏览连接真实 Java，身份、账号资料和后台浏览器测试拦截 API，以免测试向真实邮箱发送验证码或上传真实云文件；Redis、资料持久化、文件归属和管理员权限由 Java 集成测试覆盖。`e2e/profile.spec.ts` 覆盖桌面与手机端资料编辑、头像上传/移除、失败重试和键盘用户菜单。测试生成目录已加入 Git 忽略。

## 目录

- `src/app`：首页、案例、身份、账号、内容后台、加载和错误页面。
- `src/components`：站点外壳、身份表单、账号面板、后台编辑器、案例卡片、Markdown、统一提示。
- `src/lib`：API 类型、服务端案例请求、浏览器身份与后台请求和筛选条件处理。
- `e2e`：真实后端驱动的浏览器流程测试。

API JSON 响应统一为 `Result {code, msg, data, traceId}`。成功码为 `SUCCESS`；前端统一解析 `data`，失败通过 `msg` 和 `traceId` 提供提示与追踪。退出和删除成功返回 HTTP 200、`data: null`，业务失败保留相应 HTTP 状态。
