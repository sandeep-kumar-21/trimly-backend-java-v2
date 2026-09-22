package com.trimly.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Base62 Utility Unit Tests")
class Base62UtilTest {

    @Test
    @DisplayName("Should encode zero correctly")
    void shouldEncodeZero() {
        assertThat(Base62Util.encode(0)).isEqualTo("0");
        assertThat(Base62Util.decode("0")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should encode initial sequence number 100,000")
    void shouldEncodeInitialSequenceNumber() {
        long sequenceId = 100_000L;
        String encoded = Base62Util.encode(sequenceId);

        assertThat(encoded).isNotEmpty();
        assertThat(Base62Util.decode(encoded)).isEqualTo(sequenceId);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 61, 62, 3844, 999999, 1000000000L, 5000000000000L})
    @DisplayName("Should encode and decode roundtrip deterministically across ranges")
    void shouldEncodeAndDecodeRoundtrip(long value) {
        String encoded = Base62Util.encode(value);
        long decoded = Base62Util.decode(encoded);

        assertThat(decoded).isEqualTo(value);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative numbers")
    void shouldRejectNegativeNumbers() {
        assertThatThrownBy(() -> Base62Util.encode(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid Base62 characters")
    void shouldRejectInvalidBase62String() {
        assertThatThrownBy(() -> Base62Util.decode("abc-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Base62 character");
    }
}
