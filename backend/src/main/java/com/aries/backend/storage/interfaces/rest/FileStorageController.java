package com.aries.backend.storage.interfaces.rest;

import com.aries.backend.storage.application.service.FileStorageService;
import com.aries.backend.storage.domain.model.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class FileStorageController {
    private final FileStorageService service;
    public record FileView(UUID id, StoredFile.Purpose purpose, String filename, String contentType, long size) {}

    @PostMapping(value = "/uploads", consumes = "multipart/form-data")
    public ResponseEntity<FileView> upload(@RequestParam StoredFile.Purpose purpose,
                                           @RequestPart("file") MultipartFile file) throws IOException {
        try (var content = file.getInputStream()) {
            StoredFile stored = service.upload(purpose, file.getOriginalFilename(), file.getContentType(), file.getSize(), content);
            return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(new FileView(
                    stored.id(), stored.purpose(), stored.filename(), stored.contentType(), stored.size()));
        }
    }

    @GetMapping("/files/{id}/download-url")
    public ResponseEntity<FileStorageService.Download> download(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.download(id));
    }
}
