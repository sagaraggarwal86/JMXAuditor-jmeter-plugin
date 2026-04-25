package io.github.sagaraggarwal86.jmeter.jmxauditor.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScanLimitsTest {

    // Invariant 12 — the only sanctioned source for these caps.
    @Test
    void hardCapsMatchInvariant12() {
        assertThat(ScanLimits.MAX_NODES).isEqualTo(10_000);
        assertThat(ScanLimits.MAX_FINDINGS).isEqualTo(2000);
        assertThat(ScanLimits.MAX_SCAN_MILLIS).isEqualTo(10_000L);
    }
}
