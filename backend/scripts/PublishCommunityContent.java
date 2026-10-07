import cn.dev33.satoken.stp.StpUtil;

import com.aries.backend.BackendApplication;
import com.aries.backend.identity.infrastructure.persistence.mapper.UserMapper;
import com.aries.backend.identity.infrastructure.persistence.po.UserPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

/** 本地开发内容导入：真实管理员会话、媒体接口及发布用例；不直接修改内容表。 */
class PublishCommunityContent {
    static Path ROOT;
    static final URI ORIGIN = URI.create("http://127.0.0.1:8080");
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    static String token;

    static JsonNode request(String method, String path, byte[] bytes, String type)
            throws Exception {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(ORIGIN.resolve("/api/v1" + path))
                        .timeout(Duration.ofMinutes(2))
                        .header("Accept", "application/json")
                        .header("Cookie", "arieshub_token=" + token);
        if (type != null) builder.header("Content-Type", type);
        HttpResponse<String> response =
                HTTP.send(
                        builder.method(
                                        method,
                                        bytes == null
                                                ? HttpRequest.BodyPublishers.noBody()
                                                : HttpRequest.BodyPublishers.ofByteArray(bytes))
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode result = JSON.readTree(response.body());
        if (response.statusCode() >= 400 || !"SUCCESS".equals(result.path("code").asText()))
            throw new IllegalStateException(
                    method
                            + " "
                            + path
                            + " failed: "
                            + result.path("code").asText()
                            + ", traceId="
                            + result.path("traceId").asText());
        return result.path("data");
    }

    static String upload(String kind, String relative) throws Exception {
        Path file = ROOT.resolve(relative).normalize();
        if (!file.startsWith(ROOT)) throw new IllegalArgumentException("Asset outside project");
        String type = relative.endsWith(".webp") ? "image/webp" : "image/png";
        String boundary = "AriesHub" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(
                ("--"
                                + boundary
                                + "\r\nContent-Disposition: form-data; name=\"kind\"\r\n\r\n"
                                + kind
                                + "\r\n--"
                                + boundary
                                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                                + file.getFileName()
                                + "\"\r\nContent-Type: "
                                + type
                                + "\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
        body.write(Files.readAllBytes(file));
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return request(
                        "POST",
                        "/admin/media?uploadId=" + UUID.randomUUID(),
                        body.toByteArray(),
                        "multipart/form-data; boundary=" + boundary)
                .path("id")
                .asText();
    }

    public static void main(String[] args) throws Exception {
        ROOT = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(ROOT.resolve("backend/scripts/community-content.json")))
            throw new IllegalArgumentException(
                    "Run from the repository root or pass its absolute path");
        try (ConfigurableApplicationContext context =
                new SpringApplicationBuilder(BackendApplication.class)
                        .run("--server.port=0", "--spring.profiles.active=dev")) {
            UserPO admin =
                    context
                            .getBean(UserMapper.class)
                            .selectList(
                                    Wrappers.<UserPO>lambdaQuery()
                                            .eq(UserPO::getRole, "ADMIN")
                                            .eq(UserPO::getStatus, "ACTIVE")
                                            .orderByAsc(UserPO::getId))
                            .stream()
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "No active project administrator"));
            // 只创建本次导入专用会话；既有管理员会话和密码保持不变，结束后撤销此 token。
            token = StpUtil.getStpLogic().createLoginSession(admin.getId());
            try {
                JsonNode rows =
                        JSON.readTree(
                                Files.readString(
                                        ROOT.resolve("backend/scripts/community-content.json")));
                Map<String, String> categories = new HashMap<>();
                for (JsonNode c : request("GET", "/admin/categories", null, null))
                    categories.put(c.path("name").asText(), c.path("id").asText());
                Map<String, JsonNode> existing = new HashMap<>();
                for (JsonNode p : request("GET", "/admin/publications", null, null))
                    existing.put(p.path("title").asText(), p);
                ArrayNode report = JSON.createArrayNode();
                for (JsonNode row : rows) {
                    String title = row.path("title").asText();
                    if (existing.containsKey(title)) {
                        JsonNode old = existing.get(title);
                        JsonNode detail =
                                request(
                                        "GET",
                                        "/admin/publications/" + old.path("id").asText(),
                                        null,
                                        null);
                        Matcher imageReference =
                                java.util.regex.Pattern.compile("media:([0-9a-fA-F-]{36})")
                                        .matcher(detail.path("fullMarkdown").asText());
                        report.add(
                                JSON.createObjectNode()
                                        .put("id", old.path("id").asText())
                                        .put("title", title)
                                        .put("status", old.path("status").asText())
                                        .put("accessType", detail.path("accessType").asText())
                                        .put("coverFileId", detail.path("coverFileId").asText())
                                        .put(
                                                "imageFileId",
                                                imageReference.find()
                                                        ? imageReference.group(1)
                                                        : "")
                                        .put("skippedExisting", true));
                        System.out.println("SKIPPED existing " + old.path("id").asText());
                        continue;
                    }
                    String category = categories.get(row.path("theme").asText());
                    if (category == null) throw new IllegalStateException("Unknown theme");
                    String cover = upload("COVER", row.path("coverPath").asText());
                    String image = upload("IMAGE", row.path("imagePath").asText());
                    ObjectNode body = JSON.createObjectNode();
                    for (String field :
                            List.of(
                                    "title",
                                    "summary",
                                    "publicationType",
                                    "accessType",
                                    "creditPrice",
                                    "previewMarkdown",
                                    "fullMarkdown",
                                    "version",
                                    "featured")) body.set(field, row.path(field));
                    body.put("categoryId", Long.parseLong(category));
                    body.put("coverFileId", cover);
                    for (String field : List.of("previewMarkdown", "fullMarkdown"))
                        body.put(field, body.path(field).asText().replace("{{BODY_IMAGE}}", image));
                    JsonNode created =
                            request(
                                    "POST",
                                    "/admin/publications",
                                    JSON.writeValueAsBytes(body),
                                    "application/json");
                    String id = created.path("id").asText();
                    JsonNode published =
                            request("POST", "/admin/publications/" + id + "/publish", null, null);
                    request("GET", "/publications/" + id, null, null);
                    request("GET", "/publications/" + id + "/media/" + cover + "/url", null, null);
                    request("GET", "/publications/" + id + "/media/" + image + "/url", null, null);
                    report.add(
                            JSON.createObjectNode()
                                    .put("id", id)
                                    .put("title", title)
                                    .put("status", published.path("status").asText())
                                    .put("accessType", row.path("accessType").asText())
                                    .put("coverFileId", cover)
                                    .put("imageFileId", image));
                    Files.writeString(
                            ROOT.resolve("backend/scripts/community-content-published.json"),
                            JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
                    System.out.println("PUBLISHED " + id + " " + title);
                }
                Files.writeString(
                        ROOT.resolve("backend/scripts/community-content-published.json"),
                        JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
            } finally {
                StpUtil.getStpLogic().logoutByTokenValue(token);
                token = null;
            }
        }
    }
}
