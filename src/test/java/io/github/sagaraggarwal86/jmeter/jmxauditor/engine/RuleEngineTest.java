package io.github.sagaraggarwal86.jmeter.jmxauditor.engine;

import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanOutcome;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import io.github.sagaraggarwal86.jmeter.jmxauditor.util.Clock;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.threads.ThreadGroup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    @Test
    void scanOnEmptyPlanCompletesCleanlyWithNoFindings() {
        JMeterTreeModel model = JmxTestHarness.emptyPlan();
        List<Finding> publishedDuringScan = new ArrayList<>();
        ScanResult r = RuleEngine.scan(model, Set.of(), "/tmp/x.jmx", "x.jmx",
                "5.6.3", false, Clock.system(), publishedDuringScan::add);

        assertThat(r.outcome()).isEqualTo(ScanOutcome.COMPLETE);
        assertThat(r.findings()).isEmpty();
        assertThat(publishedDuringScan).isEmpty();
        assertThat(r.nodesAnalyzed()).isGreaterThanOrEqualTo(1);
        assertThat(r.suppressedRuleIds()).isEmpty();
    }

    @Test
    void scanFiresThreadGroupZeroDurationRuleOnSchedulerWithoutDuration() {
        // Rule THREAD_GROUP_ZERO_DURATION fires when scheduler=true and duration is blank/0.
        ThreadGroup tg = new ThreadGroup();
        tg.setName("TG");
        tg.setProperty("ThreadGroup.scheduler", true);
        tg.setProperty("ThreadGroup.duration", "0");

        JMeterTreeModel model = JmxTestHarness.planWithChildren(b -> {
            b.add(tg, 0);
        }).model();
        ScanResult r = RuleEngine.scan(model, Set.of(), null, "x.jmx",
                "5.6.3", false, Clock.system(), null);

        assertThat(r.findings()).extracting(Finding::ruleId).contains("THREAD_GROUP_ZERO_DURATION");
    }

    @Test
    void hiddenRuleIdsAreSuppressed() {
        JMeterTreeModel model = JmxTestHarness.emptyPlan();
        ScanResult r = RuleEngine.scan(model, Set.of("MISSING_COOKIE_MANAGER", "THREAD_GROUP_ZERO_DURATION"),
                null, "x.jmx", "5.6.3", false, Clock.system(), null);
        assertThat(r.suppressedRuleIds())
                .containsExactlyInAnyOrder("MISSING_COOKIE_MANAGER", "THREAD_GROUP_ZERO_DURATION");
    }

    @Test
    void disabledTreeBranchSkipsAllRulesExceptDisabledFlag() {
        // A disabled ThreadGroup with a scheduler-zero misconfiguration: the disabled flag
        // should suppress every rule except DISABLED_ELEMENT_IN_TREE.
        ThreadGroup tg = new ThreadGroup();
        tg.setName("Disabled TG");
        tg.setEnabled(false);
        tg.setProperty("ThreadGroup.scheduler", true);
        tg.setProperty("ThreadGroup.duration", "0");

        JMeterTreeModel model = JmxTestHarness.planWithChildren(b -> {
            b.add(tg, 0);
        }).model();
        ScanResult r = RuleEngine.scan(model, Set.of(), null, "x.jmx",
                "5.6.3", false, Clock.system(), null);

        // No correctness finding for the disabled subtree.
        assertThat(r.findings()).extracting(Finding::ruleId).doesNotContain("THREAD_GROUP_ZERO_DURATION");
        // But DISABLED_ELEMENT_IN_TREE still fires for the disabled element.
        assertThat(r.findings()).extracting(Finding::ruleId).contains("DISABLED_ELEMENT_IN_TREE");
    }
}
