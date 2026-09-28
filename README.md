# AriesHub

面向 AI 实践者的案例与数字资源平台。当前已完成落地页、数据库驱动的案例浏览、Resend 邮箱验证码身份流程，以及管理员内容后台。

## 技术与进度

- 后端：Java 27、Spring Boot 4.1.1、Sa-Token 1.46.0、Lombok 1.18.48、MyBatis-Plus 3.5.17、PostgreSQL、Redis、Flyway。
- 后端按 DDD 分为接口、应用、领域和基础设施层，按业务模块组织。
- 前端：Next.js 16.3.6、React 19.2.8、TypeScript、Tailwind CSS 4；前端 API 类型从 OpenAPI 生成。
- Sa-Token 使用 HttpOnly Cookie 保存登录凭证；密码使用 BCrypt 摘要保存。注册和登录都需要邮箱验证码，验证码摘要、冷却时间和错误次数保存在 Redis。管理员可以新建、编辑、发布和下架案例；支付和下载尚未实现。

文档：[项目规划](doc/PROJECT.md) · [后端分层约定](doc/BACKEND_ARCHITECTURE.md) · [验证码与后台验收](doc/M3_VERIFICATION_ADMIN_ACCEPTANCE.md) · [API 契约](doc/openapi.json) · [前端说明](frontend/README.md)。

## 本地启动

需要 JDK 27、已运行的 PostgreSQL 和 Redis、Node.js 和 npm。本次前端验证环境为 Node.js 26.5.0 / npm 11.17.0，建议团队统一 Node.js 24 或更高版本；Next.js 自身最低要求为 20.9。

### 1. 数据库

项目直接复用本机 `127.0.0.1:5432` 的 PostgreSQL，不启动新的数据库容器。默认账号和密码均为 `postgres`，应用使用独立数据库 `arieshub`。首次运行且数据库尚不存在时执行：

```bash
createdb -h 127.0.0.1 -p 5432 -U postgres arieshub
```

当前电脑上的数据库已创建完成。需要连接其他 PostgreSQL 时可通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 覆盖默认值。

### 2. Redis 与邮件

项目复用本机 `127.0.0.1:6379` Redis，默认密码为 `root`，不启动新的 Redis。其他环境可通过 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 覆盖。

真实发信使用 Resend。启动前配置 API Key 和已验证的发件地址，并用逗号分隔的邮箱设置首批管理员：

```bash
export RESEND_API_KEY='re_xxx'
export RESEND_FROM='AriesHub <hello@your-domain.com>'
export ADMIN_EMAILS='you@example.com'
```

未配置 Resend 时应用仍能启动，但申请验证码会返回 503，Redis 中不会遗留无法投递的验证码。`ADMIN_EMAILS` 只在新用户注册时决定初始角色，已有用户需要通过受控数据变更调整角色。

### 3. 后端

从仓库根目录启动：

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

默认监听 8080。Flyway 自动建表；显式 `dev` 配置导入三条标为演示的案例。默认配置只建表，不导入演示数据。生产使用独立数据库，不启用 dev，也不复用含演示迁移的开发数据卷。

```bash
curl http://localhost:8080/api/v1/health
curl 'http://localhost:8080/api/v1/cases?access=FREE&page=1&size=9'
curl -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","purpose":"REGISTER"}' \
  http://localhost:8080/api/v1/auth/email-codes
# 从邮箱取得六位验证码后注册
curl -i -c cookie.txt -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"hello1234","nickname":"新成员","code":"123456"}' \
  http://localhost:8080/api/v1/auth/register
curl -b cookie.txt http://localhost:8080/api/v1/auth/me
```

### 4. 前端

另开终端执行：

```bash
cd frontend
npm ci
npm run dev
```

访问 http://localhost:3000。首页、案例库、注册、登录、账号页和 `/admin` 内容后台均已接入真实 Java API；后端不可用时显示错误提示。

如需修改 Java 地址，将 `frontend/.env.example` 复制为 `frontend/.env.local`，设置 `BACKEND_ORIGIN` 后重启前端。开发时 `/api/v1/*` 同域转发已配置；生产模式下由部署网关提供该转发，生产部署尚未完成。

## 验证

后端集成测试使用 Testcontainers 的临时 PostgreSQL 和 Redis，不读写本机服务，运行时需 Docker 可用：

```bash
cd backend
./mvnw clean test
```

前端类型、格式、Lint 和生产构建：

```bash
cd frontend
npm run generate:api
npm run format:check
npm run lint
npm run typecheck
npm run build
```

浏览器测试要求后端已按 dev 配置启动，并保留默认三条演示数据。测试默认复用或启动 3000 端口前端，覆盖桌面与手机；已有前端使用其他端口时设置 `E2E_PORT`：

```bash
cd frontend
npx playwright install chromium
npm run test:e2e
```

## 当前边界

- 草稿、下架、暂停交付的案例在公开详情和正文接口返回 404。
- 列表和详情不返回完整正文。免费正文使用独立接口；付费正文统一返回 403。
- 金额按人民币分保存，ID 以字符串返回，防止前端大整数精度丢失。
- Markdown 不执行原始 HTML，不自动加载内容中的远程图片。
- 演示价格不作为实际报价，当前没有购买或文件下载。
- Sa-Token 会话当前仍使用内存存储，重启后需要重新登录；Redis 目前用于邮箱验证码。生产多实例部署前再接入 Sa-Token Redis 适配。
- 验证码请求有 60 秒冷却，验证码 10 分钟过期并限制五次错误；找回密码、密码登录频率限制和完整 CSRF 防护将在公开部署前补齐。

下一步进入内容交付：完善 Markdown 编辑体验，增加资源上传、对象存储和付费权益模型。
