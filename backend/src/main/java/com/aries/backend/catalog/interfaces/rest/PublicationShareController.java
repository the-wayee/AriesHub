package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.port.PublicationMediaPort.SignedUrl;
import com.aries.backend.catalog.application.service.PublicationMediaService;
import com.aries.backend.catalog.application.service.PublicationShareService;
import com.aries.backend.catalog.application.view.CatalogViews.Content;
import com.aries.backend.catalog.application.view.PublicationReaderViews.SharedPublication;
import com.aries.backend.shared.interfaces.rest.Result;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 分享路径本身携带单篇能力令牌；所有分支均由应用用例重新校验绑定和文章状态。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shares/{token}")
public class PublicationShareController {
    private final PublicationShareService shares;

    @GetMapping
    public Result<SharedPublication> preview(@PathVariable String token) {
        return Result.success(shares.preview(token));
    }

    @GetMapping("/content")
    public Result<Content> content(@PathVariable String token) {
        return Result.success(shares.content(token));
    }

    @GetMapping("/media/{id}/url")
    public Result<SignedUrl> media(@PathVariable String token, @PathVariable UUID id) {
        return Result.success(shares.media(token, id.toString()));
    }

    @GetMapping("/attachments")
    public Result<List<PublicationMediaService.Attachment>> attachments(
            @PathVariable String token) {
        return Result.success(shares.attachments(token));
    }
}
