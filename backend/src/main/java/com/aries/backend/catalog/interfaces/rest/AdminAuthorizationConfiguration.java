package com.aries.backend.catalog.interfaces.rest;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 管理员路由由 catalog 模块自己声明权限边界。 */
@Configuration
public class AdminAuthorizationConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            StpUtil.checkLogin();
            StpUtil.checkRole("ADMIN");
        })).addPathPatterns("/api/v1/admin/**");
    }
}
