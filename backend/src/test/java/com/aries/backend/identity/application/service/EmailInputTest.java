package com.aries.backend.identity.application.service;

import com.aries.backend.shared.application.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailInputTest {
    @Test
    void invalidEmailHasStableClientError() {
        assertThatThrownBy(() -> EmailInput.parse("a@b"))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getCode().name()).isEqualTo("INVALID_EMAIL"));
    }
}
