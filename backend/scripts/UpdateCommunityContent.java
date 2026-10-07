import cn.dev33.satoken.stp.StpUtil;
import com.aries.backend.BackendApplication;
import com.aries.backend.identity.infrastructure.persistence.mapper.UserMapper;
import com.aries.backend.identity.infrastructure.persistence.po.UserPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** 本地 Demo 编辑升级：先核对原稿，保留 ID、状态及阅读关系，通过业务接口更新。 */
class UpdateCommunityContent {
  public static void main(String[] args) throws Exception {
    if (args.length != 2 || !"--apply".equals(args[1])) {
      throw new IllegalArgumentException("Usage: UpdateCommunityContent <repository> --apply");
    }
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    PublishCommunityContent.ROOT = root;
    JsonNode rows =
        PublishCommunityContent.JSON.readTree(
            Files.readString(root.resolve("backend/scripts/community-content.json")));
    JsonNode original =
        PublishCommunityContent.JSON.readTree(
            Files.readString(
                root.resolve("backend/scripts/community-content-before-editorial.json")));
    Path reportPath = root.resolve("backend/scripts/community-content-published.json");
    ArrayNode report =
        (ArrayNode) PublishCommunityContent.JSON.readTree(Files.readString(reportPath));
    if (rows.size() != 8 || original.size() != 8 || report.size() != 8)
      throw new IllegalStateException("Expected the eight known demo articles");
    // 独立临时会话只用于本次更新，不改管理员密码，也不撤销已有会话。
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
              .orElseThrow();
      PublishCommunityContent.token = StpUtil.getStpLogic().createLoginSession(admin.getId());
      try {
        List<JsonNode> details = new ArrayList<>();
        // 全量预检必须在第一次上传之前完成，避免误覆盖用户后续编辑。
        for (int index = 0; index < rows.size(); index++) {
          JsonNode entry = report.get(index);
          JsonNode detail =
              PublishCommunityContent.request(
                  "GET", "/admin/publications/" + entry.path("id").asText(), null, null);
          String image = entry.path("imageFileId").asText();
          String previousBody =
              original
                  .get(index)
                  .path("fullMarkdown")
                  .asText()
                  .replace("{{BODY_IMAGE}}", image)
                  .trim();
          String nextBody =
              rows.get(index).path("fullMarkdown").asText().replace("{{BODY_IMAGE}}", image).trim();
          if ((!detail.path("fullMarkdown").asText().equals(previousBody)
                  && !detail.path("fullMarkdown").asText().equals(nextBody))
              || (!detail.path("title").asText().equals(original.get(index).path("title").asText())
                  && !detail.path("title").asText().equals(rows.get(index).path("title").asText())))
            throw new IllegalStateException(
                "Demo was edited separately; stopping before upload: " + entry.path("id").asText());
          if (!Files.isRegularFile(root.resolve(rows.get(index).path("coverPath").asText())))
            throw new IllegalStateException("Missing cover " + index);
          details.add(detail);
        }
        Path snapshotPath =
            root.resolve("backend/scripts/community-content-editorial-previous.json");
        if (!Files.exists(snapshotPath))
          Files.writeString(
              snapshotPath,
              PublishCommunityContent.JSON
                  .writerWithDefaultPrettyPrinter()
                  .writeValueAsString(details));
        for (int index = 0; index < rows.size(); index++) {
          JsonNode row = rows.get(index);
          JsonNode old = details.get(index);
          ObjectNode entry = (ObjectNode) report.get(index);
          String id = old.path("id").asText();
          if (old.path("title").asText().equals(row.path("title").asText())
              && entry.path("editorialUpdated").asBoolean()) continue;
          String cover = PublishCommunityContent.upload("COVER", row.path("coverPath").asText());
          ObjectNode body = PublishCommunityContent.JSON.createObjectNode();
          for (String field :
              List.of(
                  "title",
                  "summary",
                  "publicationType",
                  "accessType",
                  "creditPrice",
                  "previewMarkdown",
                  "fullMarkdown",
                  "requirements",
                  "deliverables",
                  "version",
                  "featured")) body.set(field, row.path(field));
          body.put("categoryId", Long.parseLong(old.path("categoryId").asText()));
          body.put("coverFileId", cover);
          for (String field : List.of("previewMarkdown", "fullMarkdown"))
            body.put(
                field,
                body.path(field)
                    .asText()
                    .replace("{{BODY_IMAGE}}", entry.path("imageFileId").asText()));
          JsonNode updated =
              PublishCommunityContent.request(
                  "PUT",
                  "/admin/publications/" + id,
                  PublishCommunityContent.JSON.writeValueAsBytes(body),
                  MediaType.APPLICATION_JSON_VALUE);
          if (!updated.path("status").asText().equals(old.path("status").asText()))
            throw new IllegalStateException("Publication status changed " + id);
          PublishCommunityContent.request(
              "GET", "/publications/" + id + "/media/" + cover + "/url", null, null);
          entry
              .put("title", row.path("title").asText())
              .put("coverFileId", cover)
              .put("editorialUpdated", true);
          Files.writeString(
              reportPath,
              PublishCommunityContent.JSON
                  .writerWithDefaultPrettyPrinter()
                  .writeValueAsString(report));
          System.out.println("UPDATED demo " + id);
        }
      } finally {
        StpUtil.getStpLogic().logoutByTokenValue(PublishCommunityContent.token);
        PublishCommunityContent.token = null;
      }
    }
  }
}
