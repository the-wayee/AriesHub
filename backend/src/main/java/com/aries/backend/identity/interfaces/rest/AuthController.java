package com.aries.backend.identity.interfaces.rest;

import com.aries.backend.shared.interfaces.rest.Result;
import com.aries.backend.identity.application.service.AuthApplicationService;
import com.aries.backend.identity.application.service.EmailVerificationService;
import com.aries.backend.identity.application.view.IdentityViews.CurrentUser;
import com.aries.backend.identity.interfaces.rest.request.LoginRequest;
import com.aries.backend.identity.interfaces.rest.request.RegisterRequest;
import com.aries.backend.identity.interfaces.rest.request.EmailCodeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 用户身份 HTTP 接口；登录凭证由 Sa-Token 写入 HttpOnly Cookie。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthApplicationService service;
    private final EmailVerificationService verification;

    @PostMapping("/email-codes")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<EmailVerificationService.DispatchResult> emailCode(
            @Valid @RequestBody EmailCodeRequest request) {
        return Result.success(verification.dispatch(request.email(), request.purpose()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<CurrentUser> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(service.register(request.email(), request.password(), request.nickname(), request.code()));
    }

    @PostMapping("/login")
    public Result<CurrentUser> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(service.login(request.email(), request.password()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        service.logout();
        return Result.success(null);
    }

}
