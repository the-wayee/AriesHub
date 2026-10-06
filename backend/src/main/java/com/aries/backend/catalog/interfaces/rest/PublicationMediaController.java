package com.aries.backend.catalog.interfaces.rest;

import com.aries.backend.catalog.application.port.PublicationMediaPort;
import com.aries.backend.catalog.application.service.PublicationMediaService;
import com.aries.backend.shared.interfaces.rest.Result;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicationMediaController {
    private final PublicationMediaService media;

    @PostMapping(value = "/api/v1/admin/media", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<PublicationMediaService.Uploaded> upload(
            @RequestParam String kind, @RequestPart("file") MultipartFile file) throws IOException {
        try (var content = file.getInputStream()) {
            return Result.success(
                    media.upload(
                            kind,
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getSize(),
                            content));
        }
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
