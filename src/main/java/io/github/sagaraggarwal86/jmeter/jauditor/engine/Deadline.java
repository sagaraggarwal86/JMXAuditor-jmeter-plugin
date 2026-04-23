package io.github.sagaraggarwal86.jmeter.jauditor.engine;

import io.github.sagaraggarwal86.jmeter.jauditor.util.Clock;

import java.time.Duration;
import java.time.Instant;

public final class Deadline {
    private final Clock clock;
    private final Instant at;

    public Deadline(Clock clock, Duration budget) {
        this.clock = clock;
        this.at = clock.now().plus(budget);
    }

    public boolean expired() {
        return !clock.now().isBefore(at);
    }
}
