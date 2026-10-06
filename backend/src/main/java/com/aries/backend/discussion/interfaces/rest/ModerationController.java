package com.aries.backend.discussion.interfaces.rest;

import com.aries.backend.discussion.application.service.ModerationService;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/** 跨线程审核列表入口；管理员权限由统一路由守卫检查。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/discussions")
public class ModerationController {
    private final ModerationService service;

    @GetMapping("/comments")
    public Result<ModerationService.Page> list(
            @RequestParam(defaultValue = "") @Pattern(regexp = "|PUBLISHED|HIDDEN|DELETED")
                    String status,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return Result.success(service.list(status, page, size));
    }
}
