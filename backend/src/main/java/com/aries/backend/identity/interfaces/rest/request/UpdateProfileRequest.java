package com.aries.backend.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** 只接收可编辑资料，邮箱、角色与余额不允许客户端修改。 */
public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 30) String nickname,
        @NotNull @Size(max = 160) String bio,
        UUID avatarFileId
) {}
