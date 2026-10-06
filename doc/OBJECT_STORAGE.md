# S3 / 阿里云 OSS 文件存储

所有接口返回统一 `Result {code, msg, data, traceId}`，下文的业务响应字段位于 `data` 内。

## 实现范围

独立 `storage` 模块提供通用文件写入、元数据查询和签名工具，应用层依赖 `ObjectStorage` 端口，AWS SDK 2.x 仅在基础设施中使用。`FileStorageService` 不读取会话，不决定头像、附件或视频的格式、大小及授权规则。

头像由 `identity/UserApplicationService` 负责业务校验、限流、上传、绑定资格与展示授权，经 `UserAvatarStorage` 端口和 `composition/UserAvatarStorageAdapter` 调用基础能力。以后附件、视频由对应业务领域提供独立上传接口与校验；不重新开放由浏览器选择用途的通用上传入口。通用私有下载授权集中在 `FileDownloadService`，不混入文件工具。

文件本体进入 OSS，元数据进入 PostgreSQL `stored_files`（V9）；单表 CRUD 使用 MyBatis-Plus `BaseMapper`，无 JDBC 或 XML。上传成功返回文件 ID，后续用户头像、内容附件字段引用这个 ID，不能持久化会过期的签名 URL。上传接口本身不会更新头像，也不会把附件挂到内容上。

本阶段下载权限只授予上传者。账号页已接入头像绑定与当前用户头像展示，使用单独的 inline 临时签名。后续需要由头像公开展示规则、Publication 发布状态和积分解锁权益分别授权，不能将该接口放宽成「任何登录用户凭 ID 下载」。通用下载接口继续采用 attachment 响应处置。

## 阿里云配置

1. 创建与应用同地域的 OSS Bucket，访问权限选**私有**。
2. 创建 RAM 子账号，给它这个 Bucket 下 `uploads/*` 的 `oss:PutObject`、`oss:GetObject`、`oss:DeleteObject` 权限。不需要创建 Bucket 或修改 ACL 的权限。
3. 按 Bucket 地域设置 S3 兼容 endpoint。外网格式为 `https://s3.oss-{region}.aliyuncs.com`，例如杭州为 `https://s3.oss-cn-hangzhou.aliyuncs.com`。不要填写控制台网址或普通 OSS endpoint。
4. 将仓库根目录 `.env.example` 中的配置填到本地 `.env` 或部署密钥环境中，然后通过进程环境传给后端。Spring Boot 本身不会自动读取根目录 `.env`；IDE 运行配置可配置对应环境变量，终端可先执行 `set -a; source .env; set +a` 再启动。不要提交实际密钥。

```dotenv
S3_ENABLED=true
S3_ENDPOINT=https://s3.oss-cn-hangzhou.aliyuncs.com
S3_REGION=aws-global
S3_BUCKET=your-bucket-name
S3_ACCESS_KEY_ID=
S3_ACCESS_KEY_SECRET=
S3_PATH_STYLE=false
```

`S3_REGION=aws-global` 采用阿里云官方 Java 2.x 示例的签名配置。AWS SDK 使用 V4 签名、virtual-host 地址、关闭 chunked encoding，且只在操作要求时发送额外 checksum。其他 S3 兼容供应商可以配置自己的 endpoint、签名 region 和 path-style。当前只接受 HTTPS endpoint。

阿里云官方文档提示：中国内地部分新用户的数据 API 受到默认公网域名限制，可能需要绑定自定义域名和 HTTPS 证书。实际接入前核对该 Bucket 的域名策略；不要仅替换下载 URL 的域名，签名会绑定实际请求的 host。自定义域名的 SDK 地址方式需要实际联调后确认。

未启用存储时，后端启动正常；上传和签名返回 `503 STORAGE_UNAVAILABLE`。启用后缺少必要配置会启动失败，避免错误配置直到上传时才暴露。

目前后端转发上传，浏览器不直接连接 OSS，因此不需要 OSS 的浏览器上传 CORS。以后接入大文件直传时应另做短期签名、上传大小限制、完成确认和 Bucket CORS，不把 RAM AccessKey 放进前端。

## 接口

### 上传

`POST /api/v1/users/me/avatar`，`multipart/form-data`：

- `file`：文件本体，文件 part 需设置正确的 Content-Type。
- 需要登录 Cookie。

头像支持 PNG、JPEG、WebP，最大 5 MiB；身份用例检查文件声明类型和文件头，拒绝 SVG、HTML 和任意可执行类型。文章附件、图片和视频由 catalog 领域的管理员素材接口上传，不允许使用头像接口代替。文件头检查不等同于完整图像解码或恶意文件扫描；公开头像接入时应增加重编码处理。对象名由服务端 UUID 生成，原始文件名不会参与对象路径。

每用户每分钟最多 20 次上传，Redis 原子计数在多实例共享。超限返回 `429 UPLOAD_RATE_LIMITED`，含 `Retry-After` 和 `retryAfterSeconds`。头像大小或类型校验失败为 `400 INVALID_AVATAR`；Servlet 层文件超过 100 MiB / 请求超过 101 MiB 为 `413 FILE_TOO_LARGE`。

```bash
curl -b cookie.txt -F 'file=@avatar.png;type=image/png' \
  http://localhost:8080/api/v1/users/me/avatar
```

响应 `201`：

```json
{"id":"UUID", "purpose":"AVATAR", "filename":"avatar.png", "contentType":"image/png", "size":12345}
```

对象上传成功而元数据入库失败时，会尝试删除该对象并保留原始入库异常。跨 OSS 与 PostgreSQL 无法保证原子事务：进程在两步之间中断、清理失败都可能遗留孤立对象；后续应通过文件绑定状态和定时核对清理，不能直接给整个 `uploads/` 设置到期删除规则。

### 下载

`GET /api/v1/storage/files/{id}/download-url`，需要上传者登录 Cookie，返回：

```json
{"url":"https://...signed...", "expiresAt":"2026-09-30T10:00:00Z"}
```

链接有效期 5 分钟；响应 `Cache-Control: no-store`。其他用户和不存在的文件统一返回 `404 FILE_NOT_FOUND`。临时 URL 是持有者可下载的凭据，不能把它写入公开日志或永久业务字段。下载授权发生在签名签发时，已签发 URL 到期前仍可使用。

### 账号头像绑定与展示

通过头像业务接口上传后，调用 `PUT /api/v1/users/me/profile`，请求为 `{"nickname":"昵称","bio":"个性签名","avatarFileId":"上传返回的 UUID"}`。昵称为 2–30 字，签名最多 160 字；`avatarFileId: null` 移除头像引用。服务端只允许绑定本人上传且用途为 `AVATAR` 的图片。V10 为用户添加 `bio` 与 `avatar_file_id`；当前用户响应返回这些字段，数据库不保存临时 URL。

`GET /api/v1/users/me/avatar-url` 返回当前账号头像的 `{url, expiresAt}`，有效期 5 分钟，使用 inline 响应处置与 `Cache-Control: no-store`；前端定期刷新签名。接口需要登录，只展示自己的头像，没有头像时返回 `404 AVATAR_NOT_SET`。社区中展示其他用户头像的公开读取策略尚未实现。

## 验证与后续

自动化测试用实际 PostgreSQL / Redis 容器验证元数据、会话、文件归属、限流和异常响应；对象上传替身避免使用真实云凭据。SDK 测试本地验证 OSS virtual-host 和 V4 签名生成。真实 RAM 权限、Bucket 域名策略、网络连通性和签名接受情况仍需配置后联调。

头像与 Publication 素材绑定已完成；下一步是已解锁成员的权益授权、公开头像策略、大文件直传和孤立对象清理。头像更换与附件解绑时不能直接删除可能仍被其他内容引用的对象。

依据：[阿里云 AWS SDK 接入 OSS](https://www.alibabacloud.com/help/zh/oss/developer-reference/use-aws-sdks-to-access-oss)、[AWS Java 2.x 预签名 URL](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html)。

## Publication 素材

`POST /api/v1/admin/media` 需要 ADMIN，multipart 字段 `kind` 为 COVER/IMAGE/VIDEO/ATTACHMENT，`file` 为文件。封面和图片 PNG/JPEG/WebP 最大 10 MiB；视频 MP4/WebM 最大 100 MiB；附件 PDF/ZIP/TXT/CSV/JSON/MD/PPTX/DOCX/XLSX 最大 20 MiB。图片和视频检查声明类型及文件头；附件使用 attachment 下载处置。`catalog/PublicationMediaService` 负责规则、限流、绑定与公开/付费授权；`FileStorageService` 只提供通用存取和签名。

素材响应在 Result.data 中返回 `{id,kind,filename,contentType,size,url,expiresAt}`；封面存 `coverFileId`，Markdown 存 `![图片](media:UUID)`、`![视频：文件名](media:UUID)`、`[附件：文件名](media:UUID)`。临时签名不得存入正文。V11 增加素材元数据及文章绑定，并提高通用文件表尺寸约束到 100 MiB；各业务保留自己的尺寸限制。

管理员编辑通过 `GET /api/v1/admin/media/{id}/url` 获取五分钟链接；发布后的公开展示通过 `GET /api/v1/publications/{publicationId}/media/{id}/url`。封面与 previewMarkdown 引用公开；免费正文引用允许公开阅读；积分正文私有引用当前返回 403 CONTENT_LOCKED，待积分应用用例建立后接入真实权益。草稿、下架、暂停交付和未绑定素材不可由公开接口读取。解绑立即阻止新的签名签发，已签发链接在五分钟有效期内仍可使用。

编辑其他主理人的文章可以保留既有绑定素材；不能引用尚未绑定在该文章的其他账号素材。移除绑定不直接删除对象，避免破坏其他文章引用。上传后未保存文章的素材暂时保留，孤立对象回收需后续独立作业。
