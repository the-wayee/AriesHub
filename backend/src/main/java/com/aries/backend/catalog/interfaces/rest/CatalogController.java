package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.service.CatalogQueryService;
import com.aries.backend.catalog.application.view.CatalogViews.Category;
import com.aries.backend.catalog.application.view.CatalogViews.Content;
import com.aries.backend.catalog.application.view.CatalogViews.Page;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationDetail;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;
import com.aries.backend.catalog.interfaces.rest.request.PublicationListRequest;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 公开发布内容 HTTP 接口：只做参数适配与用例调用，不直接访问数据库。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogQueryService service;

    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.success(service.categories());
    }

    @GetMapping("/publications")
    public Result<Page<PublicationSummary>> publications(
            @Valid @ModelAttribute PublicationListRequest query) {
        return Result.success(service.list(query.toQuery()));
    }

    @GetMapping("/publications/{id}")
    public Result<PublicationDetail> detail(
            @PathVariable @Pattern(regexp = "[a-z0-9-]{1,120}") String id) {
        return Result.success(service.detail(id));
    }

    @GetMapping("/publications/{id}/content")
    public Result<Content> content(@PathVariable @Positive long id) {
        return Result.success(service.memberContent(id));
    }
}
