package io.github.sagaraggarwal86.jmeter.jmxauditor.model;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScanResultTest {

    @Test
    void findingsAreDefensivelyCopied() {
        Finding f = new Finding("X", Category.CORRECTNESS, Severity.ERROR, "t", "d", "s",
                new NodePath(List.of("Test Plan")), 1);
        java.util.ArrayList<Finding> mutable = new java.util.ArrayList<>();
        mutable.add(f);
        ScanResult r = new ScanResult(Instant.EPOCH, null, "x.jmx", "5.6.3", false,
                1, 25, 0, mutable, ScanOutcome.COMPLETE, List.of(), null);
        // Mutate the source list — the result must not pick up the new entry.
        mutable.add(new Finding("Y", Category.CORRECTNESS, Severity.WARN, "t", "d", "s",
                new NodePath(List.of("X")), 1));
        assertThat(r.findings()).hasSize(1);
        assertThatThrownBy(() -> r.findings().add(f)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void suppressedRuleIdsAreDefensivelyCopied() {
        java.util.ArrayList<String> mutable = new java.util.ArrayList<>(List.of("A"));
        ScanResult r = new ScanResult(Instant.EPOCH, null, "x.jmx", "5.6.3", false,
                0, 25, 0, List.of(), ScanOutcome.COMPLETE, mutable, null);
        mutable.add("B");
        assertThat(r.suppressedRuleIds()).containsExactly("A");
    }

    @Test
    void nullNavigationBecomesEmptyMap() {
        ScanResult r = new ScanResult(Instant.EPOCH, null, "x.jmx", "5.6.3", false,
                0, 25, 0, List.of(), ScanOutcome.COMPLETE, List.of(), null);
        assertThat(r.navigation()).isEmpty();
    }

    @Test
    void navigationPreservesIdentitySemantics() {
        // Two findings with structurally-equal fields — record equals() considers them equal,
        // but the navigation map must keep them as separate entries (IdentityHashMap inside).
        NodePath path = new NodePath(List.of("Test Plan", "TG"));
        Finding a = new Finding("DUP", Category.CORRECTNESS, Severity.INFO, "t", "d", "s", path, 2);
        Finding b = new Finding("DUP", Category.CORRECTNESS, Severity.INFO, "t", "d", "s", path, 2);
        assertThat(a).isEqualTo(b); // record equality

        Map<Finding, WeakReference<org.apache.jmeter.gui.tree.JMeterTreeNode>> nav = new IdentityHashMap<>();
        nav.put(a, new WeakReference<>(null));
        nav.put(b, new WeakReference<>(null));
        ScanResult r = new ScanResult(Instant.EPOCH, null, "x.jmx", "5.6.3", false,
                0, 25, 0, List.of(a, b), ScanOutcome.COMPLETE, List.of(), nav);
        assertThat(r.navigation()).hasSize(2); // Both entries preserved.
    }

    @Test
    void hashMapNavigationStillWorks() {
        // Constructor accepts any Map and re-wraps as IdentityHashMap.
        Finding f = new Finding("X", Category.CORRECTNESS, Severity.ERROR, "t", "d", "s",
                new NodePath(List.of("Test Plan")), 1);
        Map<Finding, WeakReference<org.apache.jmeter.gui.tree.JMeterTreeNode>> nav = new HashMap<>();
        nav.put(f, new WeakReference<>(null));
        ScanResult r = new ScanResult(Instant.EPOCH, null, "x.jmx", "5.6.3", false,
                0, 25, 0, List.of(f), ScanOutcome.COMPLETE, List.of(), nav);
        assertThat(r.navigation()).hasSize(1);
    }
}
