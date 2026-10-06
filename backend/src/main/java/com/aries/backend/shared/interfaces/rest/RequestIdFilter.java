package com.aries.backend.shared.interfaces.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

/** 为每次请求生成追踪编号，避免将调用方传入的任意内容写入日志。 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE = "arieshub.requestId";
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, id);
        response.setHeader("X-Trace-Id", id);
        response.setHeader("X-Request-Id", id);
        // 禁止共享缓存，确保后续发布、下架或价格变更能在下一次请求中生效。
        if (request.getRequestURI().startsWith("/api/")) response.setHeader("Cache-Control", "no-store");
        MDC.put("traceId", id);
        try { chain.doFilter(request, response); }
        finally { MDC.remove("traceId"); }
    }
}
