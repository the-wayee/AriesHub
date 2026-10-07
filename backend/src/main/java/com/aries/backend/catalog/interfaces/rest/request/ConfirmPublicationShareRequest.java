package com.aries.backend.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 分享计数使用服务端颁发的授权令牌，客户端不能通过更换随机请求号刷次数。 */
public record ConfirmPublicationShareRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {}
