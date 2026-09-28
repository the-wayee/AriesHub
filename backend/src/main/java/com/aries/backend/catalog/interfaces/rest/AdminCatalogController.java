package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.service.AdminCatalogService;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseSummary;
import com.aries.backend.catalog.interfaces.rest.request.AdminCaseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 管理员内容接口，由 Sa-Token ADMIN 角色拦截器统一保护。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {
    private final AdminCatalogService service;

    @GetMapping("/categories")
    public List<CategoryOption> categories() {
        return service.categories();
    }

    @GetMapping("/cases")
    public List<CaseSummary> cases() {
        return service.cases();
    }

    @GetMapping("/cases/{id}")
    public CaseDetail detail(@PathVariable @Positive long id) {
        return service.detail(id);
    }

    @PostMapping("/cases")
    public ResponseEntity<CaseDetail> create(@Valid @RequestBody AdminCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.toCommand()));
    }

    @PutMapping("/cases/{id}")
    public CaseDetail update(@PathVariable @Positive long id,
                             @Valid @RequestBody AdminCaseRequest request) {
        return service.update(id, request.toCommand());
    }

    @PostMapping("/cases/{id}/publish")
    public CaseDetail publish(@PathVariable @Positive long id) {
        return service.publish(id);
    }

    @PostMapping("/cases/{id}/archive")
    public CaseDetail archive(@PathVariable @Positive long id) {
        return service.archive(id);
    }
}
