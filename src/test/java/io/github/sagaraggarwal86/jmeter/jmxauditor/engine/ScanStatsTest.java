package io.github.sagaraggarwal86.jmeter.jmxauditor.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScanStatsTest {

    @Test
    void countersStartAtZero() {
        ScanStats s = new ScanStats();
        assertThat(s.nodesVisited()).isZero();
        assertThat(s.rulesExecuted()).isZero();
        assertThat(s.findingsEmitted()).isZero();
    }

    @Test
    void incrementsAreAdditive() {
        ScanStats s = new ScanStats();
        s.incNodes();
        s.incNodes();
        s.incRules();
        s.incFindings(3);
        s.incFindings(2);
        assertThat(s.nodesVisited()).isEqualTo(2);
        assertThat(s.rulesExecuted()).isEqualTo(1);
        assertThat(s.findingsEmitted()).isEqualTo(5);
    }
}
