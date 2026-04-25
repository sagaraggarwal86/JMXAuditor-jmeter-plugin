package io.github.sagaraggarwal86.jmeter.jmxauditor.engine;

import io.github.sagaraggarwal86.jmeter.jmxauditor.support.FakeClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineTestExtended {

    @Test
    void notExpiredBeforeBudgetElapses() {
        FakeClock clock = FakeClock.atEpoch();
        Deadline d = new Deadline(clock, Duration.ofSeconds(10));
        clock.advance(Duration.ofSeconds(5));
        assertThat(d.expired()).isFalse();
    }

    @Test
    void expiresAtBudgetBoundary() {
        FakeClock clock = FakeClock.atEpoch();
        Deadline d = new Deadline(clock, Duration.ofSeconds(10));
        clock.advance(Duration.ofSeconds(10));
        // expired() returns !now.isBefore(at) — at the boundary, "before" is false.
        assertThat(d.expired()).isTrue();
    }

    @Test
    void expiresAfterBudget() {
        FakeClock clock = FakeClock.atEpoch();
        Deadline d = new Deadline(clock, Duration.ofMillis(500));
        clock.advance(Duration.ofSeconds(1));
        assertThat(d.expired()).isTrue();
    }
}
