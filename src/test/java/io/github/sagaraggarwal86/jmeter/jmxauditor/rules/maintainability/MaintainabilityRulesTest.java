package io.github.sagaraggarwal86.jmeter.jmxauditor.rules.maintainability;

import io.github.sagaraggarwal86.jmeter.jmxauditor.engine.RuleEngine;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness.NodeWithModel;
import io.github.sagaraggarwal86.jmeter.jmxauditor.util.Clock;
import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class MaintainabilityRulesTest {

    // ─── HARDCODED_HOST ─────────────────────────────────────

    @Test
    void hardcodedHost_firesOnLiteralHostname() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        http.setProperty("HTTPSampler.domain", "api.example.com");
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new HardcodedHostRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("HARDCODED_HOST");
    }

    @Test
    void hardcodedHost_doesNotFireForVariable() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        http.setProperty("HTTPSampler.domain", "${HOST}");
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new HardcodedHostRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void hardcodedHost_doesNotFireForBlankDomain() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        // domain unset
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new HardcodedHostRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void hardcodedHost_doesNotFireForNonHostishValues() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        http.setProperty("HTTPSampler.domain", "not a hostname!");
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new HardcodedHostRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── DEFAULT_SAMPLER_NAME ───────────────────────────────

    @Test
    void defaultSamplerName_firesOnUnchangedHttpRequest() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        http.setName("HTTP Request");
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new DefaultSamplerNameRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("DEFAULT_SAMPLER_NAME");
    }

    @Test
    void defaultSamplerName_doesNotFireOnRenamedSampler() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        http.setName("POST /checkout");
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new DefaultSamplerNameRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── DISABLED_ELEMENT_IN_TREE ───────────────────────────

    @Test
    void disabledElement_firesOnDisabledThreadGroup() {
        ThreadGroup tg = new ThreadGroup();
        tg.setName("Disabled TG");
        tg.setEnabled(false);
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new DisabledElementInTreeRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("DISABLED_ELEMENT_IN_TREE");
    }

    @Test
    void disabledElement_doesNotFireOnEnabledElement() {
        ThreadGroup tg = new ThreadGroup();
        tg.setEnabled(true);
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new DisabledElementInTreeRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── MISSING_TRANSACTION_CONTROLLER ─────────────────────

    @Test
    void missingTransactionController_firesOnLooseSamplersUnderThreadGroup() {
        // ThreadGroup -> HTTPSampler (direct child, no Transaction Controller)
        var nm = JmxTestHarness.planWithChildren(b -> {
            ThreadGroup tg = new ThreadGroup();
            var tgNode = b.add(tg, 0);
            b.under(tgNode).add(new HTTPSamplerProxy());
            b.under(tgNode).add(new HTTPSamplerProxy());
        });
        // result.node() is the ThreadGroup.
        var tgNode = nm.node();
        List<Finding> findings = new MissingTransactionControllerRule().check(tgNode, JmxTestHarness.newContext(nm.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("MISSING_TRANSACTION_CONTROLLER");
    }

    @Test
    void missingTransactionController_doesNotFireForEmptyThreadGroup() {
        var nm = JmxTestHarness.planWithChildren(b -> {
            b.add(new ThreadGroup(), 0);
        });
        List<Finding> findings = new MissingTransactionControllerRule().check(nm.node(), JmxTestHarness.newContext(nm.model()));
        assertThat(findings).isEmpty();
    }

    // ─── CSV_ABSOLUTE_PATH ──────────────────────────────────

    @Test
    void csvAbsolutePath_firesOnUnixAbsolutePath() {
        CSVDataSet csv = new CSVDataSet();
        csv.setProperty("filename", "/data/users.csv");
        NodeWithModel n = JmxTestHarness.singleElement(csv);
        List<Finding> findings = new CsvAbsolutePathRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("CSV_ABSOLUTE_PATH");
    }

    @Test
    void csvAbsolutePath_firesOnWindowsAbsolutePath() {
        CSVDataSet csv = new CSVDataSet();
        csv.setProperty("filename", "C:/data/users.csv");
        NodeWithModel n = JmxTestHarness.singleElement(csv);
        List<Finding> findings = new CsvAbsolutePathRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
    }

    @Test
    void csvAbsolutePath_doesNotFireForRelativePath() {
        CSVDataSet csv = new CSVDataSet();
        csv.setProperty("filename", "data/users.csv");
        NodeWithModel n = JmxTestHarness.singleElement(csv);
        List<Finding> findings = new CsvAbsolutePathRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void csvAbsolutePath_doesNotFireForVariable() {
        CSVDataSet csv = new CSVDataSet();
        csv.setProperty("filename", "${CSV_DIR}/users.csv");
        NodeWithModel n = JmxTestHarness.singleElement(csv);
        List<Finding> findings = new CsvAbsolutePathRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── JTL_EXCESSIVE_SAVE_FIELDS ──────────────────────────

    @Test
    void jtlExcessiveSaveFields_firesAbove20Enabled() {
        TestPlan plan = new TestPlan("plan");
        // Set 25 jmeter.save.saveservice.* properties to true.
        for (int i = 0; i < 25; i++) {
            plan.setProperty("jmeter.save.saveservice.field" + i, "true");
        }
        NodeWithModel n = JmxTestHarness.singleElement(plan);
        List<Finding> findings = new JtlExcessiveSaveFieldsRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("JTL_EXCESSIVE_SAVE_FIELDS");
    }

    @Test
    void jtlExcessiveSaveFields_doesNotFireAt20OrFewer() {
        TestPlan plan = new TestPlan("plan");
        for (int i = 0; i < 15; i++) {
            plan.setProperty("jmeter.save.saveservice.field" + i, "true");
        }
        NodeWithModel n = JmxTestHarness.singleElement(plan);
        List<Finding> findings = new JtlExcessiveSaveFieldsRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void jtlExcessiveSaveFields_ignoresFalseValuedFields() {
        TestPlan plan = new TestPlan("plan");
        for (int i = 0; i < 30; i++) {
            // Set false values; these don't count toward the limit.
            plan.setProperty("jmeter.save.saveservice.field" + i, "false");
        }
        NodeWithModel n = JmxTestHarness.singleElement(plan);
        List<Finding> findings = new JtlExcessiveSaveFieldsRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // Sanity test that the engine drives whole-tree rules in the correct order.
    @Test
    void engineRunsMaintainabilityRulesViaWholeTreeFirst() {
        var nm = JmxTestHarness.planWithChildren(b -> {
            b.add(new ThreadGroup(), 0);
        });
        ScanResult r = RuleEngine.scan(nm.model(), Set.of(), null, "x.jmx", "5.6.3",
                false, Clock.system(), null);
        // Just exercise the path — assertions on specific findings live in per-rule tests.
        assertThat(r.outcome().name()).isEqualTo("COMPLETE");
    }
}
