package com.aries.backend.activity.interfaces.rest;

import com.aries.backend.activity.application.query.CommunityActivityFilter;
import com.aries.backend.activity.application.service.CommunityActivityService;
import com.aries.backend.activity.application.view.CommunityActivityPage;
import com.aries.backend.shared.interfaces.rest.Result;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 社区成员的动态查询入口；Tab 筛选和滚动游标均由服务端处理。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/community")
public class CommunityActivityController {
    private final CommunityActivityService activity;

    @GetMapping("/activities")
    public Result<CommunityActivityPage> recent(
            @RequestParam(defaultValue = "12") @Min(1) @Max(24) int size,
            @RequestParam(defaultValue = "ALL") CommunityActivityFilter filter,
            @RequestParam(required = false) @Positive Long before) {
        return Result.success(activity.recent(size, filter, before));
    }
}
