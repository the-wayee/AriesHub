# AriesHub

面向 AI 实践者的案例与数字资源平台。通过自媒体展示作品，在平台提供案例预览、购买、阅读与自动交付，逐步发展为社区。

## 当前状态

- `backend/`：用户已初始化 Spring Boot 4.1.1 / Java 27 工程，包含 PostgreSQL 驱动。
- `frontend/`：已初始化 Next.js 16.3.6 / React 19.2.8 / TypeScript / Tailwind CSS 4，提供中文响应式筹备首页。
- 已确定采用 **Sa-Token + PostgreSQL**；鉴权、数据源和业务接口尚未接入。
- [项目文档](doc/PROJECT.md)：完整需求、架构、数据模型与分阶段验收。

## 启动前端

需要 Node.js 20.9+ 和 npm。本次验证环境为 Node.js 26.5.0 / npm 11.17.0。

```bash
cd frontend
npm ci
npm run dev
```

访问 http://localhost:3000。首页不依赖后端，可单独运行。

按需复制 `frontend/.env.example` 为 `frontend/.env.local`，通过 `BACKEND_ORIGIN` 修改开发代理目标，默认 `http://127.0.0.1:8080`。详细说明见 [前端 README](frontend/README.md)。

## 后端

沿用现有工程，使用 JDK 27。从仓库根目录启动的预期命令为：

```bash
cd backend
./mvnw spring-boot:run
```

本轮仅检查了后端配置，没有执行后端构建或验证其运行兼容性。数据库、健康接口和 Sa-Token 配置是后续任务；已有 PostgreSQL 驱动不代表数据库已连接。

## 前端检查

```bash
cd frontend
npm run lint
npm run typecheck
npm run build
```

构建后可运行 `npm start` 查看首页。生产 API 需由部署层将同域 `/api/v1/*` 转发至 Java，目前尚未配置。

下一步：完成 M0 的 PostgreSQL 迁移与前后端健康检查联调，再进入案例模块。
