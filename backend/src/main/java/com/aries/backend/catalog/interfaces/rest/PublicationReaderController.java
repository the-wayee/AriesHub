package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.service.PublicationCardService;
import com.aries.backend.catalog.application.service.PublicationReaderService;
import com.aries.backend.catalog.application.view.CatalogViews.Page;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Home;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Interaction;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Progress;
import com.aries.backend.catalog.application.view.PublicationReaderViews.PublicationCardView;
import com.aries.backend.catalog.domain.model.PublicationReactionKind;
import com.aries.backend.catalog.interfaces.rest.request.PublicationListRequest;
import com.aries.backend.catalog.interfaces.rest.request.ReadingProgressRequest;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户无需传 userId；幂等 PUT/DELETE 分离，避免网络重试把点赞反向切换。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PublicationReaderController {
    private final PublicationReaderService readers;
    private final PublicationCardService cards;

    @GetMapping("/publications/cards")
    public Result<Page<PublicationCardView>> list(
            @Valid @ModelAttribute PublicationListRequest query) {
        return Result.success(cards.list(query.toQuery()));
    }

    @GetMapping("/home")
    public Result<Home> home() {
        return Result.success(cards.home());
    }

    @GetMapping("/users/me/library")
    public Result<Page<PublicationCardView>> library(
            @RequestParam(defaultValue = "BOOKMARK") @Pattern(regexp = "BOOKMARK|LIKE|HISTORY")
                    String kind,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "9") @Min(1) @Max(24) int size) {
        return Result.success(cards.library(kind, page, size));
    }

    @GetMapping("/publications/{id}/interaction")
    public Result<Interaction> interaction(@PathVariable @Positive long id) {
        return Result.success(readers.interaction(id));
    }

    @PutMapping("/publications/{id}/bookmark")
    public Result<Interaction> bookmark(@PathVariable @Positive long id) {
        return Result.success(readers.reaction(id, PublicationReactionKind.BOOKMARK, true));
    }

    @DeleteMapping("/publications/{id}/bookmark")
    public Result<Interaction> unbookmark(@PathVariable @Positive long id) {
        return Result.success(readers.reaction(id, PublicationReactionKind.BOOKMARK, false));
    }

    @PutMapping("/publications/{id}/like")
    public Result<Interaction> like(@PathVariable @Positive long id) {
        return Result.success(readers.reaction(id, PublicationReactionKind.LIKE, true));
    }

    @DeleteMapping("/publications/{id}/like")
    public Result<Interaction> unlike(@PathVariable @Positive long id) {
        return Result.success(readers.reaction(id, PublicationReactionKind.LIKE, false));
    }

    @GetMapping("/publications/{id}/reading-progress")
    public Result<Progress> progress(@PathVariable @Positive long id) {
        return Result.success(readers.progress(id));
    }

    @PutMapping("/publications/{id}/reading-progress")
    public Result<Progress> progress(
            @PathVariable @Positive long id, @Valid @RequestBody ReadingProgressRequest body) {
        return Result.success(
                readers.saveProgress(id, body.version(), body.position(), body.percent()));
    }
}
