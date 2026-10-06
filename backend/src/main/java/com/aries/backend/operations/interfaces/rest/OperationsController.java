package com.aries.backend.operations.interfaces.rest;

import com.aries.backend.operations.application.service.OperationsService;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/operations")
public class OperationsController {
    private final OperationsService service;

    @GetMapping("/overview")
    public Result<OperationsService.Overview> overview() {
        return Result.success(service.overview());
    }

    @GetMapping("/ledger")
    public Result<OperationsService.LedgerPage> ledger(
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return Result.success(service.ledger(page, size));
    }
}
