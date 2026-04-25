package io.github.sagaraggarwal86.jmeter.jmxauditor.export;

import io.github.sagaraggarwal86.jmeter.jmxauditor.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAggregatesTest {

    private static Finding finding(String id, Category c, Severity s) {
        return new Finding(id, c, s, "t", "d", "x", new NodePath(List.of("Test Plan")), 1);
    }

    private static ScanResult scanResult(List<Finding> findings, List<String> suppressed) {
        return new ScanResult(Instant.EPOCH, null, "x.jmx", "5.6.3", false,
                1, 25, 0, findings, ScanOutcome.COMPLETE, suppressed, null);
    }

    @Test
    void emptyResultProducesZeroCountsAcrossAllCategories() {
        ScanResult r = scanResult(List.of(), List.of());
        ReportAggregates agg = ReportAggregates.of(r);

        for (Category c : Category.values()) {
            assertThat(agg.byCategory().get(c)).isZero();
            assertThat(agg.findingsByCategory().get(c)).isEmpty();
        }
        for (Severity s : Severity.values()) {
            assertThat(agg.bySeverity().get(s)).isZero();
        }
        // Rule reference still has 25 entries even with no findings.
        assertThat(agg.rules()).hasSize(25);
    }

    @Test
    void countsSplitAcrossCategoryAndSeverity() {
        Finding a = finding("RULE1", Category.CORRECTNESS, Severity.ERROR);
        Finding b = finding("RULE2", Category.CORRECTNESS, Severity.WARN);
        Finding c = finding("RULE3", Category.SECURITY, Severity.ERROR);
        Finding d = finding("RULE4", Category.OBSERVABILITY, Severity.INFO);
        ScanResult r = scanResult(List.of(a, b, c, d), List.of());
        ReportAggregates agg = ReportAggregates.of(r);

        assertThat(agg.byCategory().get(Category.CORRECTNESS)).isEqualTo(2);
        assertThat(agg.byCategory().get(Category.SECURITY)).isEqualTo(1);
        assertThat(agg.byCategory().get(Category.OBSERVABILITY)).isEqualTo(1);
        assertThat(agg.byCategory().get(Category.SCALABILITY)).isZero();

        assertThat(agg.bySeverity().get(Severity.ERROR)).isEqualTo(2);
        assertThat(agg.bySeverity().get(Severity.WARN)).isEqualTo(1);
        assertThat(agg.bySeverity().get(Severity.INFO)).isEqualTo(1);

        assertThat(agg.findingsByCategory().get(Category.CORRECTNESS)).containsExactly(a, b);
    }

    @Test
    void suppressedRuleIdsFlowThroughToRuleRows() {
        ScanResult r = scanResult(List.of(), List.of("MISSING_COOKIE_MANAGER", "BEANSHELL_USAGE"));
        ReportAggregates agg = ReportAggregates.of(r);
        assertThat(agg.rules())
                .filteredOn(ReportAggregates.RuleRow::suppressed)
                .extracting(ReportAggregates.RuleRow::id)
                .containsExactlyInAnyOrder("MISSING_COOKIE_MANAGER", "BEANSHELL_USAGE");
    }
}
