package io.github.sagaraggarwal86.jmeter.jmxauditor.rules.correctness;

import io.github.sagaraggarwal86.jmeter.jmxauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness.NodeWithModel;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.extractor.BoundaryExtractor;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.threads.ThreadGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

class CorrectnessRulesTest {

    // ─── EXTRACTOR_NO_DEFAULT ───────────────────────────────

    @Test
    void extractorNoDefault_firesOnRegexExtractorWithEmptyDefault() {
        RegexExtractor ext = new RegexExtractor();
        ext.setName("Extract token");
        ext.setProperty("RegexExtractor.refname", "token");
        // Default is implicitly empty.
        NodeWithModel n = JmxTestHarness.singleElement(ext);
        ScanContext ctx = JmxTestHarness.newContext(n.model());
        List<Finding> findings = new ExtractorNoDefaultRule().check(n.node(), ctx);
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("EXTRACTOR_NO_DEFAULT");
    }

    @Test
    void extractorNoDefault_doesNotFireWhenDefaultPresent() {
        RegexExtractor ext = new RegexExtractor();
        ext.setProperty("RegexExtractor.default", "NOT_FOUND");
        NodeWithModel n = JmxTestHarness.singleElement(ext);
        List<Finding> findings = new ExtractorNoDefaultRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void extractorNoDefault_doesNotFireWhenDefaultEmptyValueFlagSet() {
        RegexExtractor ext = new RegexExtractor();
        ext.setProperty("RegexExtractor.default_empty_value", true);
        NodeWithModel n = JmxTestHarness.singleElement(ext);
        List<Finding> findings = new ExtractorNoDefaultRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void extractorNoDefault_firesOnJsonPostProcessorWithEmptyDefault() {
        JSONPostProcessor jpp = new JSONPostProcessor();
        jpp.setRefNames("user_id");
        // No default values set.
        NodeWithModel n = JmxTestHarness.singleElement(jpp);
        List<Finding> findings = new ExtractorNoDefaultRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
    }

    @Test
    void extractorNoDefault_firesOnBoundaryExtractorWithEmptyDefault() {
        BoundaryExtractor be = new BoundaryExtractor();
        be.setRefName("session_id");
        NodeWithModel n = JmxTestHarness.singleElement(be);
        List<Finding> findings = new ExtractorNoDefaultRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
    }

    @Test
    void extractorNoDefault_doesNotFireWhenBoundaryEmptyValueFlagSet() {
        BoundaryExtractor be = new BoundaryExtractor();
        be.setProperty("BoundaryExtractor.default_empty_value", true);
        NodeWithModel n = JmxTestHarness.singleElement(be);
        List<Finding> findings = new ExtractorNoDefaultRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── THREAD_GROUP_ZERO_DURATION ─────────────────────────

    @Test
    void threadGroupZeroDuration_firesWhenSchedulerOnAndDurationZero() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.scheduler", true);
        tg.setProperty("ThreadGroup.duration", "0");
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new ThreadGroupZeroDurationRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("THREAD_GROUP_ZERO_DURATION");
    }

    @Test
    void threadGroupZeroDuration_firesWhenSchedulerOnAndDurationBlank() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.scheduler", true);
        // duration unset → blank
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new ThreadGroupZeroDurationRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
    }

    @Test
    void threadGroupZeroDuration_doesNotFireWhenSchedulerOff() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.scheduler", false);
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new ThreadGroupZeroDurationRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void threadGroupZeroDuration_doesNotFireWithPositiveDuration() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.scheduler", true);
        tg.setProperty("ThreadGroup.duration", "300");
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new ThreadGroupZeroDurationRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── ASSERTION_SCOPE_MISMATCH ───────────────────────────

    @Test
    void assertionScopeMismatch_firesOnParentSamplerWithEmbeddedResources() {
        // TestPlan -> HTTP sampler (image_parser=true) -> ResponseAssertion (scope blank)
        ResponseAssertion ra = new ResponseAssertion();
        // scope unset → blank → treated as "Main sample only".
        NodeWithModel result = JmxTestHarness.planWithChildren(b -> {
            HTTPSamplerProxy http = new HTTPSamplerProxy();
            http.setName("HTTP");
            http.setProperty("HTTPSampler.image_parser", true);
            org.apache.jmeter.gui.tree.JMeterTreeNode httpNode = b.add(http, 0);
            // Add the assertion as a child of the sampler.
            b.under(httpNode).add(ra);
        });
        // result.node() is the HTTP sampler — we want the assertion node.
        org.apache.jmeter.gui.tree.JMeterTreeNode httpNode = result.node();
        org.apache.jmeter.gui.tree.JMeterTreeNode assertionNode =
                (org.apache.jmeter.gui.tree.JMeterTreeNode) httpNode.getChildAt(0);
        ScanContext ctx = JmxTestHarness.newContext(result.model());
        List<Finding> findings = new AssertionScopeMismatchRule().check(assertionNode, ctx);
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("ASSERTION_SCOPE_MISMATCH");
    }

    @Test
    void assertionScopeMismatch_doesNotFireWhenScopeIsAll() {
        ResponseAssertion ra = new ResponseAssertion();
        ra.setProperty("Assertion.scope", "all");
        NodeWithModel result = JmxTestHarness.planWithChildren(b -> {
            HTTPSamplerProxy http = new HTTPSamplerProxy();
            http.setProperty("HTTPSampler.image_parser", true);
            var httpNode = b.add(http, 0);
            b.under(httpNode).add(ra);
        });
        var httpNode = result.node();
        var assertionNode = (org.apache.jmeter.gui.tree.JMeterTreeNode) httpNode.getChildAt(0);
        List<Finding> findings = new AssertionScopeMismatchRule().check(assertionNode, JmxTestHarness.newContext(result.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void assertionScopeMismatch_doesNotFireWhenSamplerHasNoEmbeddedResources() {
        ResponseAssertion ra = new ResponseAssertion();
        NodeWithModel result = JmxTestHarness.planWithChildren(b -> {
            HTTPSamplerProxy http = new HTTPSamplerProxy();
            // image_parser unset → false
            var httpNode = b.add(http, 0);
            b.under(httpNode).add(ra);
        });
        var assertionNode = (org.apache.jmeter.gui.tree.JMeterTreeNode) result.node().getChildAt(0);
        List<Finding> findings = new AssertionScopeMismatchRule().check(assertionNode, JmxTestHarness.newContext(result.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void assertionScopeMismatch_doesNotFireForOrphanAssertion() {
        ResponseAssertion ra = new ResponseAssertion();
        // Direct child of TestPlan — parent is TestPlan, not a Sampler.
        NodeWithModel n = JmxTestHarness.singleElement(ra);
        List<Finding> findings = new AssertionScopeMismatchRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── EXTRACTOR_NO_REFERENCE_NAME ────────────────────────

    @Test
    void extractorNoReferenceName_firesWhenRefNameBlank() {
        RegexExtractor ext = new RegexExtractor();
        // refname unset
        NodeWithModel n = JmxTestHarness.singleElement(ext);
        List<Finding> findings = new ExtractorNoReferenceNameRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("EXTRACTOR_NO_REFERENCE_NAME");
    }

    @Test
    void extractorNoReferenceName_doesNotFireWhenRefNamePresent() {
        RegexExtractor ext = new RegexExtractor();
        ext.setProperty("RegexExtractor.refname", "myVar");
        NodeWithModel n = JmxTestHarness.singleElement(ext);
        List<Finding> findings = new ExtractorNoReferenceNameRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void extractorNoReferenceName_firesForJsonPostProcessor() {
        JSONPostProcessor jpp = new JSONPostProcessor();
        // refnames unset
        NodeWithModel n = JmxTestHarness.singleElement(jpp);
        List<Finding> findings = new ExtractorNoReferenceNameRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
    }

    @Test
    void extractorNoReferenceName_firesForBoundaryExtractor() {
        BoundaryExtractor be = new BoundaryExtractor();
        NodeWithModel n = JmxTestHarness.singleElement(be);
        List<Finding> findings = new ExtractorNoReferenceNameRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
    }
}
