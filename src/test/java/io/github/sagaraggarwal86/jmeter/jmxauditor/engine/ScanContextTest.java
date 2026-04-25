package io.github.sagaraggarwal86.jmeter.jmxauditor.engine;

import io.github.sagaraggarwal86.jmeter.jmxauditor.support.FakeClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

class ScanContextTest {

    @Test
    void memoizeComputesOnce() {
        FakeClock clock = FakeClock.atEpoch();
        // tree is intentionally null — memoize() never touches it. Passing a real
        // JMeterTreeModel requires JMeterUtils.setJMeterHome(...) bootstrap, which
        // is deferred to the Tier 1 fixture harness (see CLAUDE.md Testing table).
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
