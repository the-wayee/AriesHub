package com.aries.backend.identity.interfaces.rest;

import com.aries.backend.identity.application.port.AdminUserPort;
import com.aries.backend.identity.application.service.AdminUserService;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AdminUserService users;

    public record StatusRequest(@NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status) {}

    @GetMapping
    public Result<AdminUserService.Page> list(
            @RequestParam(defaultValue = "") @Size(max = 120) String q,
            @RequestParam(defaultValue = "") @Pattern(regexp = "|ACTIVE|DISABLED") String status,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return Result.success(users.list(q, status, page, size));
    }

    @PutMapping("/{id}/status")
    public Result<AdminUserPort.User> changeStatus(
            @PathVariable @Positive long id, @Valid @RequestBody StatusRequest request) {
        return Result.success(users.changeStatus(id, request.status()));
    }
}
