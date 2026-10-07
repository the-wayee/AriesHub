package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.service.AdminCatalogService;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.AdminPublicationSummary;
import com.aries.backend.catalog.interfaces.rest.request.AdminPublicationRequest;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理员内容接口，由 Sa-Token ADMIN 角色拦截器统一保护。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {
    private final AdminCatalogService service;

    @GetMapping("/publications")
    public Result<List<AdminPublicationSummary>> publications() {
        return Result.success(service.publications());
    }

    @GetMapping("/publications/{id}")
    public Result<AdminPublicationDetail> detail(@PathVariable @Positive long id) {
        return Result.success(service.detail(id));
    }

    @PostMapping("/publications")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<AdminPublicationDetail> create(
            @Valid @RequestBody AdminPublicationRequest request) {
        return Result.success(service.create(request.toCommand()));
    }

    @PutMapping("/publications/{id}")
    public Result<AdminPublicationDetail> update(
            @PathVariable @Positive long id, @Valid @RequestBody AdminPublicationRequest request) {
        return Result.success(service.update(id, request.toCommand()));
    }

    @PostMapping("/publications/{id}/publish")
    public Result<AdminPublicationDetail> publish(@PathVariable @Positive long id) {
        return Result.success(service.publish(id));
    }

    @PostMapping("/publications/{id}/archive")
    public Result<AdminPublicationDetail> archive(@PathVariable @Positive long id) {
        return Result.success(service.archive(id));
    }
}
