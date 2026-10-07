package com.aries.backend.catalog.interfaces.rest.request;

import com.aries.backend.catalog.application.command.SavePublicationCommand;
import com.aries.backend.catalog.domain.model.Publication.AccessType;
import com.aries.backend.catalog.domain.model.Publication.PublicationType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 后台发布内容编辑参数；积分价格使用非负整数。 */
public record AdminPublicationRequest(
        @Positive long categoryId,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 500) String summary,
        @NotBlank @Pattern(regexp = PublicationType.VALID_VALUES) String publicationType,
        @NotBlank @Pattern(regexp = AccessType.VALID_VALUES) String accessType,
        @PositiveOrZero long creditPrice,
        @Size(max = 100_000) String previewMarkdown,
        @NotNull @Size(max = 500_000) String fullMarkdown,
        // 兼容旧客户端；正文修订标识由服务端生成，不接受作者指定。
        @Size(max = 32) String version,
        @Pattern(regexp = "[0-9a-fA-F-]{36}") String coverFileId,
        Boolean featured,
        @Size(max = 20)
                List<@NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}") String> attachmentIds) {
    @AssertTrue(message = "免费内容积分价格必须为 0，积分内容价格必须大于 0")
    public boolean isPriceValid() {
        return (AccessType.FREE.name().equals(accessType) && creditPrice == 0)
                || (AccessType.CREDIT.name().equals(accessType) && creditPrice > 0);
    }

    public SavePublicationCommand toCommand() {
        return new SavePublicationCommand(
                categoryId,
                title.trim(),
                summary.trim(),
                publicationType,
                accessType,
                creditPrice,
                previewMarkdown == null ? null : previewMarkdown.trim(),
                fullMarkdown.trim(),
                coverFileId,
                Boolean.TRUE.equals(featured),
                attachmentIds);
    }
}
