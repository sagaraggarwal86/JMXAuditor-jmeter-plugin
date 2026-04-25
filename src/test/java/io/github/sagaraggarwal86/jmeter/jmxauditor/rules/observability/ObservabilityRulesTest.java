package io.github.sagaraggarwal86.jmeter.jmxauditor.rules.observability;

import io.github.sagaraggarwal86.jmeter.jmxauditor.engine.RuleEngine;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness.NodeWithModel;
import io.github.sagaraggarwal86.jmeter.jmxauditor.util.Clock;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.java.sampler.JSR223Sampler;
import org.apache.jmeter.threads.ThreadGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityRulesTest {

    // ─── HTTP_SAMPLER_NO_ASSERTION ──────────────────────────

    @Test
    void httpNoAssertion_firesWhenNoAssertionAnywhere() {
        // TestPlan -> TG -> HTTP (no assertion under it or any ancestor).
        var nm = JmxTestHarness.planWithChildren(b -> {
            ThreadGroup tg = new ThreadGroup();
            var tgNode = b.add(tg, 0);
            b.under(tgNode).add(new HTTPSamplerProxy());
        });
        var httpNode = (org.apache.jmeter.gui.tree.JMeterTreeNode) nm.node().getChildAt(0);
        List<Finding> findings = new HttpSamplerNoAssertionRule().check(httpNode, JmxTestHarness.newContext(nm.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("HTTP_SAMPLER_NO_ASSERTION");
    }

    @Test
    void httpNoAssertion_doesNotFireWhenAssertionAttachedToSampler() {
        var nm = JmxTestHarness.planWithChildren(b -> {
            ThreadGroup tg = new ThreadGroup();
            var tgNode = b.add(tg, 0);
            HTTPSamplerProxy http = new HTTPSamplerProxy();
            var httpNode = b.under(tgNode).add(http);
            b.under(httpNode).add(new ResponseAssertion());
        });
        var httpNode = (org.apache.jmeter.gui.tree.JMeterTreeNode) nm.node().getChildAt(0);
        List<Finding> findings = new HttpSamplerNoAssertionRule().check(httpNode, JmxTestHarness.newContext(nm.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void httpNoAssertion_doesNotFireWhenAssertionOnAncestor() {
        var nm = JmxTestHarness.planWithChildren(b -> {
            ThreadGroup tg = new ThreadGroup();
            var tgNode = b.add(tg, 0);
            // Assertion at the Thread Group level — applies to children.
            b.under(tgNode).add(new ResponseAssertion());
            b.under(tgNode).add(new HTTPSamplerProxy());
        });
        // Find the HTTP child of the TG; it's the second child after the assertion.
        var tgNode = nm.node();
        org.apache.jmeter.gui.tree.JMeterTreeNode httpNode = null;
        for (int i = 0; i < tgNode.getChildCount(); i++) {
            var child = (org.apache.jmeter.gui.tree.JMeterTreeNode) tgNode.getChildAt(i);
            if (child.getTestElement() instanceof HTTPSamplerProxy) {
                httpNode = child;
                break;
            }
        }
        assertThat(httpNode).isNotNull();
        List<Finding> findings = new HttpSamplerNoAssertionRule().check(httpNode, JmxTestHarness.newContext(nm.model()));
        assertThat(findings).isEmpty();
    }

    // ─── UNNAMED_TRANSACTION_CONTROLLER ─────────────────────

    @Test
    void unnamedTransactionController_firesOnDefaultName() {
        TransactionController tc = new TransactionController();
        tc.setName("Transaction Controller");
        NodeWithModel n = JmxTestHarness.singleElement(tc);
        List<Finding> findings = new UnnamedTransactionControllerRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("UNNAMED_TRANSACTION_CONTROLLER");
    }

    @Test
    void unnamedTransactionController_doesNotFireOnRenamedController() {
        TransactionController tc = new TransactionController();
        tc.setName("Checkout Flow");
        NodeWithModel n = JmxTestHarness.singleElement(tc);
        List<Finding> findings = new UnnamedTransactionControllerRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── TRANSACTION_PARENT_SAMPLE ──────────────────────────

    @Test
    void transactionParent_firesWhenParentSampleOff() {
        TransactionController tc = new TransactionController();
        tc.setProperty("TransactionController.parent", false);
        NodeWithModel n = JmxTestHarness.singleElement(tc);
        List<Finding> findings = new TransactionParentSampleRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("TRANSACTION_PARENT_SAMPLE");
    }

    @Test
    void transactionParent_doesNotFireWhenParentSampleOn() {
        TransactionController tc = new TransactionController();
        tc.setProperty("TransactionController.parent", true);
        NodeWithModel n = JmxTestHarness.singleElement(tc);
        List<Finding> findings = new TransactionParentSampleRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── JSR223_NO_CACHE_KEY ────────────────────────────────

    @Test
    void jsr223NoCacheKey_firesOnScriptWithoutCacheKey() {
        JSR223Sampler s = new JSR223Sampler();
        s.setProperty("script", "log.info('hi')");
        // cacheKey unset.
        NodeWithModel n = JmxTestHarness.singleElement(s);
        List<Finding> findings = new Jsr223NoCacheKeyRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("JSR223_NO_CACHE_KEY");
    }

    @Test
    void jsr223NoCacheKey_doesNotFireWhenCacheKeySet() {
        JSR223Sampler s = new JSR223Sampler();
        s.setProperty("script", "x");
        s.setProperty("cacheKey", "my_script_v1");
        NodeWithModel n = JmxTestHarness.singleElement(s);
        List<Finding> findings = new Jsr223NoCacheKeyRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void jsr223NoCacheKey_doesNotFireWhenScriptBlank() {
        JSR223Sampler s = new JSR223Sampler();
        // script unset = blank
        NodeWithModel n = JmxTestHarness.singleElement(s);
        List<Finding> findings = new Jsr223NoCacheKeyRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void jsr223NoCacheKey_doesNotFireForNonJsr223Element() {
        // A plain ThreadGroup whose class name doesn't contain "JSR223".
        org.apache.jmeter.threads.ThreadGroup tg = new org.apache.jmeter.threads.ThreadGroup();
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new Jsr223NoCacheKeyRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void engineRunsObservabilityCategory() {
        var nm = JmxTestHarness.planWithChildren(b -> {
            b.add(new ThreadGroup(), 0);
        });
        ScanResult r = RuleEngine.scan(nm.model(), Set.of(), null, "x.jmx", "5.6.3",
                false, Clock.system(), null);
        assertThat(r.outcome().name()).isEqualTo("COMPLETE");
    }
}
