package io.github.sagaraggarwal86.jmeter.jauditor.engine;

import io.github.sagaraggarwal86.jmeter.jauditor.support.FakeClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ScanContextTest {

    @Test
    void memoizeComputesOnce() {
        FakeClock clock = FakeClock.atEpoch();
        ScanContext ctx = new ScanContext(null, new ScanStats(),
                new Deadline(clock, Duration.ofSeconds(10)), clock);
        AtomicInteger calls = new AtomicInteger();
        String a = ctx.memoize("k", () -> {
            calls.incrementAndGet();
            return "v";
        });
        String b = ctx.memoize("k", () -> {
            calls.incrementAndGet();
            return "v2";
        });
        assertThat(a).isEqualTo("v");
        assertThat(b).isEqualTo("v");
        assertThat(calls.get()).isEqualTo(1);
    }
}
