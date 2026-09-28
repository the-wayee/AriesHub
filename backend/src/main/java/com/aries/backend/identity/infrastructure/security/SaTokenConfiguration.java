package com.aries.backend.identity.infrastructure.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.aries.backend.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Sa-Token 路由鉴权与角色读取配置。 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminEmailProperties.class)
public class SaTokenConfiguration implements WebMvcConfigurer {
    private final UserRepository users;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/v1/auth/me", "/api/v1/auth/logout");
        registry.addInterceptor(new SaInterceptor(handle -> {
                    StpUtil.checkLogin();
                    StpUtil.checkRole("ADMIN");
                }))
                .addPathPatterns("/api/v1/admin/**");
    }

    @Bean
    StpInterface stpInterface() {
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return List.of();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                try {
                    long id = Long.parseLong(loginId.toString());
                    return users.findById(id)
                            .map(user -> List.of(user.getRole().name()))
                            .orElseGet(List::of);
                } catch (NumberFormatException ignored) {
                    return List.of();
                }
            }
        };
    }
}
