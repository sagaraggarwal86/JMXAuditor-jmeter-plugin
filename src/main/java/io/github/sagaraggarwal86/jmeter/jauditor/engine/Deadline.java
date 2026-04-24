package io.github.sagaraggarwal86.jmeter.jauditor.engine;

import io.github.sagaraggarwal86.jmeter.jauditor.util.Clock;

import java.time.Duration;
import java.time.Instant;

/**
 * Wall-clock expiration check driven by an injected {@link Clock}. Used by
 * {@link TreeWalker} to abort a scan that has exceeded {@link ScanLimits#MAX_SCAN_MILLIS}.
 * The Clock injection lets {@code DeadlineTest} exercise expiration deterministically
 * via {@code FakeClock}.
 */
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
