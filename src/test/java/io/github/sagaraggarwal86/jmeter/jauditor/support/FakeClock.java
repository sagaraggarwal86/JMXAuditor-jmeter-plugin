package io.github.sagaraggarwal86.jmeter.jauditor.support;

import io.github.sagaraggarwal86.jmeter.jauditor.util.Clock;

import java.time.Duration;
import java.time.Instant;

public final class FakeClock implements Clock {
    private Instant now;

    public FakeClock(Instant start) {
        this.now = start;
    }

    public static FakeClock atEpoch() {
        return new FakeClock(Instant.ofEpochSecond(0));
    }

    @Override
    public Instant now() {
        return now;
    }

    public void advance(Duration d) {
        now = now.plus(d);
    }
}
