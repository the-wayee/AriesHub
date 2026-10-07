package com.aries.backend.inspiration.interfaces.rest;

import com.aries.backend.inspiration.application.service.PhilosophyQuoteService;
import com.aries.backend.inspiration.application.view.PhilosophyQuoteView;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 匿名接口只读缓存；未预热时成功返回 data=null，前端省略装饰性文案。 */
@RestController
@RequestMapping("/api/v1/inspiration")
public class PhilosophyQuoteController {
    private final PhilosophyQuoteService service;

    public PhilosophyQuoteController(PhilosophyQuoteService service) {
        this.service = service;
    }

    @GetMapping("/quote")
    public Result<PhilosophyQuoteView> random(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return Result.success(service.random());
    }
}
