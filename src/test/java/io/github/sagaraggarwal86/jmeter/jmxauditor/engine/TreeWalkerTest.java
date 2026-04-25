package io.github.sagaraggarwal86.jmeter.jmxauditor.engine;

import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanOutcome;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.FakeClock;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.threads.ThreadGroup;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TreeWalkerTest {

    private static ThreadGroup thread(String name) {
        ThreadGroup tg = new ThreadGroup();
        tg.setName(name);
        return tg;
    }

    @Test
    void visitsEveryNodeInDfsOrder() {
        // Build TestPlan -> 3 ThreadGroups.
        JMeterTreeModel model = JmxTestHarness.planWithChildren(b -> {
            b.add(thread("TG1"), 0);
            b.add(thread("TG2"), 1);
            b.add(thread("TG3"), 2);
        }).model();
        ScanContext ctx = JmxTestHarness.newContext(model);
        JMeterTreeNode planNode = (JMeterTreeNode) ((JMeterTreeNode) model.getRoot()).getChildAt(0);

        List<String> visited = new ArrayList<>();
        TreeWalker.WalkResult result = TreeWalker.walk(planNode, ctx,
                (n, idx) -> visited.add(n.getName()));

        assertThat(result.abortReason()).isEqualTo(TreeWalker.AbortReason.NONE);
        assertThat(result.nodesVisited()).isEqualTo(4);
        assertThat(visited).containsExactly("Test Plan", "TG1", "TG2", "TG3");
    }

    @Test
    void abortsOnInterruptedThread() {
        JMeterTreeModel model = JmxTestHarness.planWithChildren(b -> {
            b.add(thread("TG"), 0);
        }).model();
        ScanContext ctx = JmxTestHarness.newContext(model);
        JMeterTreeNode planNode = (JMeterTreeNode) ((JMeterTreeNode) model.getRoot()).getChildAt(0);

        Thread.currentThread().interrupt();
        try {
            TreeWalker.WalkResult result = TreeWalker.walk(planNode, ctx, (n, idx) -> {
            });
            assertThat(result.abortReason()).isEqualTo(TreeWalker.AbortReason.CANCELLED);
        } finally {
            // Clean up: walk consumes the interrupt flag via Thread.interrupted(), but be safe.
            Thread.interrupted();
        }
    }

    @Test
    void abortsOnDeadline() {
        JMeterTreeModel model = JmxTestHarness.planWithChildren(b -> {
            b.add(thread("TG"), 0);
        }).model();
        FakeClock clock = FakeClock.atEpoch();
        // Build the context (and Deadline) FIRST at clock=0 so the budget anchors there;
        // THEN jump the clock past the budget. Order matters — Deadline captures
        // `at = clock.now().plus(budget)` at construction time.
        ScanContext ctx = JmxTestHarness.newContext(model, clock);
        clock.advance(Duration.ofSeconds(20));
        JMeterTreeNode planNode = (JMeterTreeNode) ((JMeterTreeNode) model.getRoot()).getChildAt(0);

        TreeWalker.WalkResult result = TreeWalker.walk(planNode, ctx, (n, idx) -> {
        });
        assertThat(result.abortReason()).isEqualTo(TreeWalker.AbortReason.TIMEOUT);
    }

    @Test
    void abortsOnFindingLimit() {
        JMeterTreeModel model = JmxTestHarness.planWithChildren(b -> {
            b.add(thread("TG"), 0);
        }).model();
        ScanContext ctx = JmxTestHarness.newContext(model);
        // Saturate the finding counter before the walk starts.
        ctx.stats().incFindings(ScanLimits.MAX_FINDINGS);
        JMeterTreeNode planNode = (JMeterTreeNode) ((JMeterTreeNode) model.getRoot()).getChildAt(0);

        TreeWalker.WalkResult result = TreeWalker.walk(planNode, ctx, (n, idx) -> {
        });
        assertThat(result.abortReason()).isEqualTo(TreeWalker.AbortReason.FINDING_LIMIT);
    }

    @Test
    void mapOutcomeCoversEveryReason() {
        // mapOutcome is a tiny pure function — exhaustive enum coverage so the
        // switch can never silently fall through if a new AbortReason is added.
        AtomicInteger seen = new AtomicInteger();
        for (TreeWalker.AbortReason r : TreeWalker.AbortReason.values()) {
            ScanOutcome out = TreeWalker.mapOutcome(r);
            assertThat(out).isNotNull();
            seen.incrementAndGet();
        }
        assertThat(seen.get()).isEqualTo(TreeWalker.AbortReason.values().length);
        // Spot-check the wiring:
        assertThat(TreeWalker.mapOutcome(TreeWalker.AbortReason.NONE)).isEqualTo(ScanOutcome.COMPLETE);
        assertThat(TreeWalker.mapOutcome(TreeWalker.AbortReason.CANCELLED)).isEqualTo(ScanOutcome.CANCELLED);
        assertThat(TreeWalker.mapOutcome(TreeWalker.AbortReason.TIMEOUT)).isEqualTo(ScanOutcome.TIMEOUT);
        assertThat(TreeWalker.mapOutcome(TreeWalker.AbortReason.NODE_LIMIT)).isEqualTo(ScanOutcome.NODE_LIMIT);
        assertThat(TreeWalker.mapOutcome(TreeWalker.AbortReason.FINDING_LIMIT)).isEqualTo(ScanOutcome.FINDING_LIMIT);
    }
}
