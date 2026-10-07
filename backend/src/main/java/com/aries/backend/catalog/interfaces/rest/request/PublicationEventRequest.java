package com.aries.backend.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** UUID 既可标识匿名读者，也可作为一次分享的幂等请求号。 */
public record PublicationEventRequest(
        @NotBlank
                @Pattern(
                        regexp =
                                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")
                String token) {}
