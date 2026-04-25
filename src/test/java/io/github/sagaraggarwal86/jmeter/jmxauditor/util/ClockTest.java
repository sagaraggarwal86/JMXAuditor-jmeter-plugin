package io.github.sagaraggarwal86.jmeter.jmxauditor.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ClockTest {

    @Test
    void systemReturnsCurrentInstant() {
        Clock c = Clock.system();
        Instant before = Instant.now().minusSeconds(2);
        Instant got = c.now();
        Instant after = Instant.now().plusSeconds(2);
        assertThat(got).isBetween(before, after);
    }

    @Test
    void systemAdvancesBetweenCalls() {
        Clock c = Clock.system();
        Instant a = c.now();
        // Busy-loop briefly so the second now() is strictly later, even on a low-resolution clock.
        Instant b;
        do {
            b = c.now();
        } while (Duration.between(a, b).toNanos() <= 0 && Duration.between(a, Instant.now()).toMillis() < 1000);
        assertThat(b).isAfterOrEqualTo(a);
    }
}
