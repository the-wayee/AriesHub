package com.aries.backend.storage.interfaces.rest;

import com.aries.backend.shared.interfaces.rest.Result;
import com.aries.backend.storage.application.service.FileStorageService;
import com.aries.backend.storage.application.service.FileDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class FileStorageController {
    private final FileDownloadService service;

    @GetMapping("/files/{id}/download-url")
    public Result<FileStorageService.Download> download(@PathVariable UUID id) {
        return Result.success(service.download(id));
    }
}
