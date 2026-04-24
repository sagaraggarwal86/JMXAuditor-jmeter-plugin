package io.github.sagaraggarwal86.jmeter.jauditor.util;

import java.time.Instant;

/**
 * Minimal testable time source. Production code uses {@link #system()}; tests inject
 * {@code FakeClock} to exercise
 * {@link io.github.sagaraggarwal86.jmeter.jauditor.engine.Deadline} deterministically.
 */
public interface Clock {
    static Clock system() {
        return Instant::now;
    }

    Instant now();
}
