package com.aries.backend.identity.interfaces.rest;

import cn.dev33.satoken.stp.StpUtil;
import com.aries.backend.identity.application.service.AuthApplicationService;
import com.aries.backend.identity.application.view.IdentityViews.CurrentUser;
import com.aries.backend.identity.interfaces.rest.request.LoginRequest;
import com.aries.backend.identity.interfaces.rest.request.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 用户身份 HTTP 接口；登录凭证由 Sa-Token 写入 HttpOnly Cookie。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthApplicationService service;

    @PostMapping("/register")
    public ResponseEntity<CurrentUser> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.register(request.email(), request.password(), request.nickname()));
    }

    @PostMapping("/login")
    public CurrentUser login(@Valid @RequestBody LoginRequest request) {
        return service.login(request.email(), request.password());
    }

    @GetMapping("/me")
    public CurrentUser me() {
        return service.currentUser();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        StpUtil.logout();
        return ResponseEntity.noContent().build();
    }
}
