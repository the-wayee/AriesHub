package com.aries.backend.identity.interfaces.rest;

import com.aries.backend.identity.application.service.AuthTrafficGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 在请求体解析前限制来源请求数，格式错误的请求也计入限额。 */
@Configuration
@RequiredArgsConstructor
public class AuthRateLimitConfiguration implements WebMvcConfigurer {
    private final AuthTrafficGuard guard;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if ("POST".equals(request.getMethod())) checkSource(request);
                return true;
            }
        }).addPathPatterns("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/email-codes");
    }

    private void checkSource(HttpServletRequest request) {
        // 不信任客户端可伪造的 X-Forwarded-For；部署网关须在可信边界处理真实来源。
        String address = request.getRemoteAddr();
        switch (request.getRequestURI().substring(request.getContextPath().length())) {
            case "/api/v1/auth/login" -> guard.checkLoginSource(address);
            case "/api/v1/auth/register" -> guard.checkRegisterSource(address);
            case "/api/v1/auth/email-codes" -> guard.checkCodeSource(address);
            default -> { }
        }
    }
}
