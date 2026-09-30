package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.service.AdminCatalogService;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationSummary;
import com.aries.backend.catalog.interfaces.rest.request.AdminPublicationRequest;
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

    @GetMapping("/publications")
    public List<AdminPublicationSummary> publications() {
        return service.publications();
    }

    @GetMapping("/publications/{id}")
    public AdminPublicationDetail detail(@PathVariable @Positive long id) {
        return service.detail(id);
    }

    @PostMapping("/publications")
    public ResponseEntity<AdminPublicationDetail> create(@Valid @RequestBody AdminPublicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.toCommand()));
    }

    @PutMapping("/publications/{id}")
    public AdminPublicationDetail update(@PathVariable @Positive long id,
                             @Valid @RequestBody AdminPublicationRequest request) {
        return service.update(id, request.toCommand());
    }

    @PostMapping("/publications/{id}/publish")
    public AdminPublicationDetail publish(@PathVariable @Positive long id) {
        return service.publish(id);
    }

    @PostMapping("/publications/{id}/archive")
    public AdminPublicationDetail archive(@PathVariable @Positive long id) {
        return service.archive(id);
    }
}
