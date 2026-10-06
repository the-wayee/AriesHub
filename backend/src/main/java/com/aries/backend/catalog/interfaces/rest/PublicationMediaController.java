package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.port.PublicationMediaPort;
import com.aries.backend.catalog.application.port.PublicationUploadProgress;
import com.aries.backend.catalog.application.service.PublicationMediaService;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/** 管理员素材上传与签名接口；公开素材签名必须通过内容领域授权。 */
@RestController
@RequiredArgsConstructor
public class PublicationMediaController {
    private final PublicationMediaService media;

    @PostMapping(value = "/api/v1/admin/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Result<PublicationMediaService.Uploaded> upload(
            @RequestParam String kind,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) UUID uploadId)
            throws IOException {
        try (var content = file.getInputStream()) {
            return Result.success(
                    media.upload(
                            kind,
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getSize(),
                            content,
                            uploadId));
        }
    }

    @GetMapping("/api/v1/admin/media/uploads/{id}")
    public Result<PublicationUploadProgress.Status> uploadStatus(
            @PathVariable UUID id, HttpServletResponse response) {
        response.setHeader(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store");
        return Result.success(media.uploadStatus(id));
    }

    @DeleteMapping("/api/v1/admin/media/uploads/{id}")
    public Result<Void> cancelUpload(@PathVariable UUID id) {
        media.cancelUpload(id);
        return Result.success(null);
    }

    @GetMapping("/api/v1/admin/media/{id}/url")
    public Result<PublicationMediaPort.SignedUrl> adminUrl(@PathVariable UUID id) {
        return Result.success(media.adminUrl(id.toString()));
    }

    @GetMapping("/api/v1/publications/{publicationId}/media/{id}/url")
    public Result<PublicationMediaPort.SignedUrl> publicUrl(
            @PathVariable @jakarta.validation.constraints.Positive long publicationId,
            @PathVariable UUID id) {
        return Result.success(media.publicUrl(publicationId, id.toString()));
    }
}
