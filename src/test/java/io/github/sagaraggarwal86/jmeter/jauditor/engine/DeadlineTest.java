package io.github.sagaraggarwal86.jmeter.jauditor.engine;

import io.github.sagaraggarwal86.jmeter.jauditor.support.FakeClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineTest {

    @Test
    void expiresAfterBudget() {
        FakeClock clock = new FakeClock(Instant.ofEpochSecond(100));
        Deadline d = new Deadline(clock, Duration.ofSeconds(10));
        assertThat(d.expired()).isFalse();
        clock.advance(Duration.ofSeconds(9));
        assertThat(d.expired()).isFalse();
        clock.advance(Duration.ofSeconds(2));
        assertThat(d.expired()).isTrue();
    }
}
