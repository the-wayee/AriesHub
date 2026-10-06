package com.aries.backend.storage.interfaces.rest;

import com.aries.backend.storage.application.service.FileStorageService;
import com.aries.backend.storage.application.service.FileDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class FileStorageController {
    private final FileDownloadService service;

    @GetMapping("/files/{id}/download-url")
    public ResponseEntity<FileStorageService.Download> download(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.download(id));
    }
}
