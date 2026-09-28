package com.aries.backend.catalog.interfaces.rest.request;

import com.aries.backend.catalog.application.command.SaveCaseCommand;
import jakarta.validation.constraints.*;

/** 后台案例编辑参数；金额单位为人民币分。 */
public record AdminCaseRequest(
        @Positive long categoryId,
        @NotBlank @Pattern(regexp = "[a-z0-9-]{1,120}") String slug,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 500) String summary,
        @NotBlank @Pattern(regexp = "FREE|PAID") String accessType,
        @PositiveOrZero long priceMinor,
        @NotBlank @Size(max = 100_000) String previewMarkdown,
        @NotBlank @Size(max = 500_000) String fullMarkdown,
        @NotBlank @Size(max = 2_000) String requirements,
        @NotBlank @Size(max = 2_000) String deliverables,
        @NotBlank @Size(max = 32) String version
) {
    @AssertTrue(message = "免费案例价格必须为 0，付费案例价格必须大于 0")
    public boolean isPriceValid() {
        return ("FREE".equals(accessType) && priceMinor == 0) ||
                ("PAID".equals(accessType) && priceMinor > 0);
    }

    public SaveCaseCommand toCommand() {
        return new SaveCaseCommand(categoryId, slug.trim(), title.trim(), summary.trim(), accessType,
                priceMinor, previewMarkdown.trim(), fullMarkdown.trim(), requirements.trim(),
                deliverables.trim(), version.trim());
    }
}
