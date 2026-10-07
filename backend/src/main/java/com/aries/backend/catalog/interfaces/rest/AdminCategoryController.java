package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.service.AdminCategoryService;
import com.aries.backend.catalog.application.view.CategoryViews.CategoryOption;
import com.aries.backend.catalog.interfaces.rest.request.CreateCategoryRequest;
import com.aries.backend.catalog.interfaces.rest.request.ReorderCategoriesRequest;
import com.aries.backend.catalog.interfaces.rest.request.UpdateCategoryRequest;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 分类读取与创建沿用 /admin 的 ADMIN 权限守卫和统一返回体。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {
    private final AdminCategoryService service;

    @PutMapping("/{id}")
    public Result<CategoryOption> update(
            @PathVariable long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return Result.success(service.update(id, request.name(), request.color()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable long id) {
        service.delete(id);
        return Result.success(null);
    }

    @GetMapping
    public Result<List<CategoryOption>> categories() {
        return Result.success(service.categories());
    }

    @PutMapping("/order")
    public Result<List<CategoryOption>> reorder(
            @Valid @RequestBody ReorderCategoriesRequest request) {
        return Result.success(service.reorder(request.categoryIds()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<CategoryOption> create(@Valid @RequestBody CreateCategoryRequest request) {
        return Result.success(service.create(request.name(), request.sortOrder(), request.color()));
    }
}
