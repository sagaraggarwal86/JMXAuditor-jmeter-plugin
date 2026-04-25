package io.github.sagaraggarwal86.jmeter.jmxauditor.rules;

import io.github.sagaraggarwal86.jmeter.jmxauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.ThreadGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractRuleTest {

    @Test
    void propStringHandlesMissingAndPresent() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.num_threads", "10");
        Probe probe = new Probe();
        assertThat(probe.callPropString(tg, "ThreadGroup.num_threads")).isEqualTo("10");
        assertThat(probe.callPropString(tg, "missing.key")).isEqualTo("");
        // Null TestElement returns empty string defensively.
        assertThat(probe.callPropString(null, "k")).isEqualTo("");
    }

    @Test
    void propBoolReadsTrueLiterals() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("flag", true);
        Probe probe = new Probe();
        assertThat(probe.callPropBool(tg, "flag")).isTrue();
        assertThat(probe.callPropBool(tg, "missing")).isFalse();
        assertThat(probe.callPropBool(null, "k")).isFalse();
    }

    @Test
    void propIntFallsBackToDefaultOnBlankOrNonInt() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("good", "42");
        tg.setProperty("bad", "not-a-number");
        Probe probe = new Probe();
        assertThat(probe.callPropInt(tg, "good", 99)).isEqualTo(42);
        assertThat(probe.callPropInt(tg, "missing", 99)).isEqualTo(99);
        assertThat(probe.callPropInt(tg, "bad", 99)).isEqualTo(99);
    }

    @Test
    void hasJMeterVarMatchesDollarBrace() {
        Probe probe = new Probe();
        assertThat(probe.callHasJMeterVar("${HOST}")).isTrue();
        assertThat(probe.callHasJMeterVar("prefix-${TOKEN}-suffix")).isTrue();
        assertThat(probe.callHasJMeterVar("plain")).isFalse();
        assertThat(probe.callHasJMeterVar(null)).isFalse();
    }

    @Test
    void allNodesWalksTheWholeTree() {
        // TestPlan -> TG; allNodes returns synthetic-root + TestPlan + TG.
        JMeterTreeModel model = JmxTestHarness.planWithChildren(b -> {
            ThreadGroup tg = new ThreadGroup();
            tg.setName("TG");
            b.add(tg, 0);
        }).model();
        Probe probe = new Probe();
        List<JMeterTreeNode> nodes = probe.callAllNodes(model);
        assertThat(nodes).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void makeProducesFindingWithRuleMetadata() {
        JMeterTreeModel model = JmxTestHarness.emptyPlan();
        ScanContext ctx = JmxTestHarness.newContext(model);
        JMeterTreeNode planNode = (JMeterTreeNode) ((JMeterTreeNode) model.getRoot()).getChildAt(0);
        Probe probe = new Probe();
        Finding f = probe.callMake(ctx.pathFor(planNode), "T", "D", "S");
        assertThat(f.ruleId()).isEqualTo("PROBE");
        assertThat(f.category()).isEqualTo(Category.CORRECTNESS);
        assertThat(f.severity()).isEqualTo(Severity.INFO);
        assertThat(f.title()).isEqualTo("T");
        assertThat(f.description()).isEqualTo("D");
        assertThat(f.suggestion()).isEqualTo("S");
    }

    /**
     * Tiny rule subclass that exposes the protected helpers under test. Lives in this
     * test only — production code should never grow a "Probe" rule.
     */
    private static final class Probe extends AbstractRule {
        @Override
        public String id() {
            return "PROBE";
        }

        @Override
        public Category category() {
            return Category.CORRECTNESS;
        }

        @Override
        public Severity severity() {
            return Severity.INFO;
        }

        @Override
        public String description() {
            return "test";
        }

        @Override
        public Set<Class<? extends TestElement>> appliesTo() {
            return Set.of();
        }

        @Override
        public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
            return List.of();
        }

        String callPropString(TestElement te, String key) {
            return propString(te, key);
        }

        boolean callPropBool(TestElement te, String key) {
            return propBool(te, key);
        }

        int callPropInt(TestElement te, String key, int def) {
            return propInt(te, key, def);
        }

        boolean callHasJMeterVar(String s) {
            return hasJMeterVar(s);
        }

        List<JMeterTreeNode> callAllNodes(JMeterTreeModel m) {
            return allNodes(m);
        }

        Finding callMake(io.github.sagaraggarwal86.jmeter.jmxauditor.model.NodePath p,
                         String t, String d, String s) {
            return make(p, t, d, s);
        }
    }
}
