package com.aries.backend.identity.interfaces.rest;

import com.aries.backend.identity.application.service.UserApplicationService;
import com.aries.backend.identity.application.port.UserAvatarStorage;
import com.aries.backend.identity.application.view.IdentityViews.CurrentUser;
import com.aries.backend.identity.interfaces.rest.request.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

/** 当前用户及其个人资料接口。 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {
    private final UserApplicationService service;
    public record UploadedAvatar(UUID id, String purpose, String filename, String contentType, long size) {}

    @GetMapping
    public CurrentUser me() {
        return service.currentUser();
    }

    @PutMapping("/profile")
    public CurrentUser updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return service.updateProfile(request.nickname(), request.bio(), request.avatarFileId());
    }

    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    public ResponseEntity<UploadedAvatar> uploadAvatar(@RequestPart("file") MultipartFile file) throws IOException {
        try (var content = file.getInputStream()) {
            var uploaded = service.uploadAvatar(file.getOriginalFilename(), file.getContentType(), file.getSize(), content);
            return ResponseEntity.status(201).body(new UploadedAvatar(uploaded.id(), uploaded.purpose(), uploaded.filename(), uploaded.contentType(), uploaded.size()));
        }
    }

    @GetMapping("/avatar-url")
    public UserAvatarStorage.Image avatarImage() {
        return service.avatarImage();
    }
}
