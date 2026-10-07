# AriesHub

面向 AI 实践者的内容社区。主理人发布实战案例、文章与课程，成员围绕内容阅读、评论和交流；完整内容使用积分解锁。

## 技术与进度

- 后端：Java 27、Spring Boot 4.1.1、Sa-Token 1.46.0、Lombok 1.18.48、MyBatis-Plus 3.5.17、PostgreSQL、Redis、Flyway。数据库访问统一经过 MyBatis-Plus Mapper，复杂 SQL 保存在 XML Mapper。
- 后端按 DDD 分为接口、应用、领域和基础设施层，按业务模块组织。
- 前端：Next.js 16.3.6、React 19.2.8、TypeScript、Tailwind CSS 4；前端 API 类型从 OpenAPI 生成。
- Sa-Token 使用 HttpOnly Cookie 保存浏览器凭证，服务端登录态、验证码及认证限流保存在 Redis；密码使用 BCrypt 摘要保存在 PostgreSQL。管理员可以新建、编辑、发布和下架当前内容；评论支持根评论分页、单层回复、点赞，以及作者自删、管理员隐藏与锁帖。积分余额与内容价格已落到 `users` / `publications`，流水与解锁表已建立；积分应用接口与支付尚未实现。

文档：[项目规划](doc/PROJECT.md) · [后端分层约定](doc/BACKEND_ARCHITECTURE.md) · [验证码与后台验收](doc/M3_VERIFICATION_ADMIN_ACCEPTANCE.md) · [对象存储配置](doc/OBJECT_STORAGE.md) · [API 契约](doc/openapi.json) · [前端说明](frontend/README.md)。

## 本地启动

需要 JDK 27、已运行的 PostgreSQL 和 Redis、Node.js 和 npm。本次前端验证环境为 Node.js 26.5.0 / npm 11.17.0，建议团队统一 Node.js 24 或更高版本；Next.js 自身最低要求为 20.9。

### 1. 数据库

项目复用现有 `postgres` 容器中的 PostgreSQL 15，映射到本机 `127.0.0.1:5432`。默认账号和密码均为 `postgres`，应用使用独立数据库 `arieshub`。容器停止时执行 `docker start postgres`；首次运行且数据库尚不存在时执行：

```bash
docker exec -e PGPASSWORD=postgres postgres createdb -h 127.0.0.1 -U postgres -O postgres arieshub
```

当前电脑上的数据库已创建完成。需要连接其他 PostgreSQL 时可通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 覆盖默认值。

### 2. Redis 与邮件

项目使用 `compose.yaml` 中的 `arieshub-redis` 容器，映射到本机 `127.0.0.1:6379`，密码为 `root`。在仓库根目录启动并验证（首次启动需要能够拉取 Redis 镜像）：

```bash
docker compose up -d redis
docker exec -e REDISCLI_AUTH=root arieshub-redis redis-cli ping
```

返回 `PONG` 表示可用。Redis 启用 AOF 持久化，数据保存在 `arieshub-redis-data` 卷，容器使用 `unless-stopped` 重启策略。其他环境可通过 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 覆盖。

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

如果 Maven Central 返回 `403 Forbidden`，可显式使用项目提供的阿里云 Central 镜像配置。Spring Boot `4.1.1` 已发布；此错误是仓库访问失败，保留 POM 中的版本与 `<relativePath/>`。在仓库根目录用 PowerShell 执行：

```powershell
# Wrapper 下载 Maven 本身时也使用镜像；仅影响当前终端。
$env:MVNW_REPOURL = 'https://maven.aliyun.com/repository/central'
.\backend\mvnw.cmd -s backend/.mvn/settings.xml -f backend/pom.xml -U validate
.\backend\mvnw.cmd -s backend/.mvn/settings.xml -f backend/pom.xml spring-boot:run '-Dspring-boot.run.profiles=dev'
```

`-U` 强制重试之前失败并被缓存的依赖下载。`-s` 必须显式传入，Maven 不会自动加载 `.mvn/settings.xml`。IntelliJ IDEA 中，将 Maven 的 **User settings file** 设置为此仓库的 `backend/.mvn/settings.xml`，然后重新加载 Maven 项目。镜像配置仅替换 Central；如果已有用户级代理、认证或镜像配置，应将此文件中的 `<mirror>` 合并到原 settings 文件，再使用合并后的文件。

```bash
curl http://localhost:8080/api/v1/health
curl 'http://localhost:8080/api/v1/publications?type=CASE_STUDY&access=FREE&page=1&size=9'
curl -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","purpose":"REGISTER"}' \
  http://localhost:8080/api/v1/auth/email-codes
# 从邮箱取得六位验证码后注册
curl -i -c cookie.txt -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"hello1234","nickname":"新成员","code":"123456"}' \
  http://localhost:8080/api/v1/auth/register
curl -b cookie.txt http://localhost:8080/api/v1/users/me
```

### 4. 前端

另开终端执行：

```bash
cd frontend
npm ci
npm run dev
```

访问 http://localhost:3200。公开首页、注册、登录、账号页和 `/admin` 内容后台已接入真实 Java API；成员区为新版社区体验，前端讨论页仍是本地原型，尚未调用真实评论接口。

账号设置从右上角用户菜单进入，不占用社区导航 tab。支持上传或移除 OSS 头像、修改昵称与个性签名；资料保存后同步到用户菜单。头像支持 PNG、JPEG、WebP，最大 5 MiB，存储配置见 [对象存储说明](doc/OBJECT_STORAGE.md)。

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

浏览器测试要求后端已按 dev 配置启动，并保留默认三条演示数据。测试默认复用或启动 3200 端口前端，覆盖桌面与手机；已有前端使用其他端口时设置 `E2E_PORT`：

```bash
cd frontend
npx playwright install chromium
npm run test:e2e
```

## 当前边界

- 草稿、下架、暂停交付的内容在公开详情和正文接口返回 404。
- 列表和详情不返回完整正文。免费正文使用独立接口；未解锁的积分内容统一返回 403。
- 内容使用 `Publication` 模型，并区分实战案例、文章和课程；价格统一使用整数积分，ID 以字符串返回以防止前端大整数精度丢失。
- Markdown 不执行原始 HTML，不自动加载内容中的远程图片。
- 前端积分数值是体验数据，当前不会扣除积分，也没有充值或文件下载。
- 评论回复是单层的：根评论分页，每条带回复总数与前两条预览，「展开全部」再分页拉取。作者删除自己的评论会保留占位行，其下回复继续可见；管理员隐藏则整条不可见。
- Sa-Token 使用官方 Redis DAO，登录态可跨应用重启及多实例共享；仍受总有效期、活跃超时、主动退出和 Redis 数据保留策略约束。
- 验证码请求有 60 秒冷却，验证码 10 分钟过期并限制五次错误。Redis 来源限额分别为登录每 10 分钟 60 次、注册每小时 30 次、发码每小时 60 次；规范化邮箱限额分别为登录每 15 分钟 5 次、注册每 15 分钟 5 次、发码每小时 5 次，成功登录会清除该邮箱的登录计数。找回密码和完整 CSRF 防护仍需在公开部署前补齐。当前不信任客户端转发头；同一反向代理后的用户会共享来源限额，公开部署时还应在可信网关按真实客户端地址限流。

当前先完善 UI 与账号基础；之后把新版文章详情和讨论页接入真实内容与评论接口，随后实现积分余额操作、流水与内容解锁，支付放在积分体系稳定之后。

API JSON 响应统一为 `Result {code, msg, data, traceId}`。成功码为 `SUCCESS`；前端统一解析 `data`，失败通过 `msg` 和 `traceId` 提供提示与追踪。退出和删除成功返回 HTTP 200、`data: null`，业务失败保留相应 HTTP 状态。

文章分享域名统一由项目级环境变量 `SITE_URL` 配置（本地默认 `http://localhost:3200`）。将根目录 `.env.example` 中的变量注入后端启动环境，生产改为 HTTPS 正式域名并重启后端；前端通过分享链接接口读取地址，无需重新构建。详见 [文章互动与分享配置](doc/PUBLICATION_INTERACTIONS.md)。
