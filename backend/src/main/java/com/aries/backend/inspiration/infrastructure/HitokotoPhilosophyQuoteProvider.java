package com.aries.backend.inspiration.infrastructure;

import com.aries.backend.inspiration.application.port.PhilosophyQuoteProvider;
import com.aries.backend.inspiration.application.view.PhilosophyQuoteView;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;

/** 外部供应商仅由缓存任务调用，固定哲学分类，不接受前端传入 URL。 */
@Component
public class HitokotoPhilosophyQuoteProvider implements PhilosophyQuoteProvider {
    private static final URI ENDPOINT =
            URI.create("https://v1.hitokoto.cn/?c=k&encode=json&min_length=8&max_length=30");
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    @Override
    public PhilosophyQuoteView fetch() {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            JsonNode data = json.readTree(response.body());
            String text = text(data, "hitokoto");
            if (!"k".equals(text(data, "type")) || text == null || text.length() > 100) return null;
            String source =
                    Stream.of(text(data, "from_who"), text(data, "from"))
                            .filter(value -> value != null)
                            .distinct()
                            .reduce((left, right) -> left + " · " + right)
                            .orElse(null);
            return new PhilosophyQuoteView(
                    text,
                    source == null ? null : source.substring(0, Math.min(80, source.length())));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("文案拉取被中断", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("文案上游暂不可用", exception);
        }
    }

    private String text(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value != null && value.isString() && !value.asString().isBlank()
                ? value.asString().trim()
                : null;
    }
}
