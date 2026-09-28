package com.aries.backend.shared.interfaces.rest;

import com.aries.backend.shared.infrastructure.health.DatabaseProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 技术健康接口不属于业务领域；不暴露数据库地址、账户或异常细节。 */
@RestController
@RequiredArgsConstructor
public class HealthController {
    private final DatabaseProbe database;
    public record Health(String status, String database) {}

    @GetMapping("/api/v1/health")
    public Health health() {
        database.verify();
        return new Health("UP", "UP");
    }
}
