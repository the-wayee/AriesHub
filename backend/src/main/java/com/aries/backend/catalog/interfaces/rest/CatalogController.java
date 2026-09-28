package com.aries.backend.catalog.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.aries.backend.catalog.application.service.CatalogQueryService;
import com.aries.backend.catalog.interfaces.rest.request.CaseListRequest;
import static com.aries.backend.catalog.application.view.CatalogViews.*;

/** 公开案例 HTTP 接口：只做参数适配与用例调用，不直接访问数据库。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogQueryService service;

    @GetMapping("/categories")
    public List<Category> categories() { return service.categories(); }

    @GetMapping("/cases")
    public Page<CaseSummary> cases(@Valid @ModelAttribute CaseListRequest query) { return service.list(query.toQuery()); }

    @GetMapping("/cases/{slug}")
    public CaseDetail detail(@PathVariable @Pattern(regexp = "[a-z0-9-]{1,120}") String slug) {
        return service.detail(slug);
    }

    @GetMapping("/cases/{id}/content")
    public Content content(@PathVariable @Positive long id) { return service.content(id); }
}
