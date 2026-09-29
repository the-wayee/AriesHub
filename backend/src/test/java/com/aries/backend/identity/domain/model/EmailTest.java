package com.aries.backend.identity.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EmailTest {
    @Test
    void normalizesBeforeComparingAddresses() {
        assertThat(new Email("  MEMBER@Example.COM ")).isEqualTo(new Email("member@example.com"));
    }

    @Test
    void rejectsInvalidAddresses() {
        assertThatThrownBy(() -> new Email("not-an-email")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
