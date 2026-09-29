package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.service.AdminCatalogService;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminCaseDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminCaseSummary;
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
    public List<AdminCaseSummary> cases() {
        return service.cases();
    }

    @GetMapping("/cases/{id}")
    public AdminCaseDetail detail(@PathVariable @Positive long id) {
        return service.detail(id);
    }

    @PostMapping("/cases")
    public ResponseEntity<AdminCaseDetail> create(@Valid @RequestBody AdminCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.toCommand()));
    }

    @PutMapping("/cases/{id}")
    public AdminCaseDetail update(@PathVariable @Positive long id,
                             @Valid @RequestBody AdminCaseRequest request) {
        return service.update(id, request.toCommand());
    }

    @PostMapping("/cases/{id}/publish")
    public AdminCaseDetail publish(@PathVariable @Positive long id) {
        return service.publish(id);
    }

    @PostMapping("/cases/{id}/archive")
    public AdminCaseDetail archive(@PathVariable @Positive long id) {
        return service.archive(id);
    }
}
