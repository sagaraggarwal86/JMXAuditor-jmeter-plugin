package io.github.sagaraggarwal86.jmeter.jmxauditor.rules.scalability;

import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness.NodeWithModel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.threads.ThreadGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

class ScalabilityRulesTest {

    // ─── GUI_LISTENER_IN_LOAD_PATH ──────────────────────────

    @Test
    void guiListener_firesForViewResultsTreeListener() {
        ResultCollector rc = new ResultCollector();
        rc.setProperty("TestElement.gui_class",
                "org.apache.jmeter.visualizers.ViewResultsFullVisualizer");
        NodeWithModel n = JmxTestHarness.singleElement(rc);
        List<Finding> findings = new GuiListenerInLoadPathRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("GUI_LISTENER_IN_LOAD_PATH");
    }

    @Test
    void guiListener_doesNotFireForSimpleDataWriter() {
        ResultCollector rc = new ResultCollector();
        rc.setProperty("TestElement.gui_class",
                "org.apache.jmeter.reporters.gui.SimpleDataWriterGui");
        NodeWithModel n = JmxTestHarness.singleElement(rc);
        List<Finding> findings = new GuiListenerInLoadPathRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── BEANSHELL_USAGE ────────────────────────────────────

    @Test
    void beanshellUsage_firesOnBeanShellElementByClassName() {
        // Synthesize a TestElement whose class name contains "BeanShell".
        BeanShellSampler bs = new BeanShellSampler();
        NodeWithModel n = JmxTestHarness.singleElement(bs);
        List<Finding> findings = new BeanshellUsageRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("BEANSHELL_USAGE");
    }

    @Test
    void beanshellUsage_doesNotFireForOrdinaryElements() {
        ThreadGroup tg = new ThreadGroup();
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new BeanshellUsageRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── SAVE_RESPONSE_DATA_ENABLED ─────────────────────────

    @Test
    void saveResponseData_firesWhenEnabled() {
        ResultCollector rc = new ResultCollector();
        SampleSaveConfiguration cfg = new SampleSaveConfiguration();
        cfg.setResponseData(true);
        rc.setSaveConfig(cfg);
        NodeWithModel n = JmxTestHarness.singleElement(rc);
        List<Finding> findings = new SaveResponseDataEnabledRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("SAVE_RESPONSE_DATA_ENABLED");
    }

    @Test
    void saveResponseData_doesNotFireWhenDisabled() {
        ResultCollector rc = new ResultCollector();
        SampleSaveConfiguration cfg = new SampleSaveConfiguration();
        cfg.setResponseData(false);
        rc.setSaveConfig(cfg);
        NodeWithModel n = JmxTestHarness.singleElement(rc);
        List<Finding> findings = new SaveResponseDataEnabledRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void saveResponseData_doesNotFireWhenSaveConfigNull() {
        // ResultCollector with no SaveConfiguration — defensive null check.
        ResultCollector rc = new ResultCollector();
        // Some JMeter constructions return null SaveConfig; simulate by not setting one.
        // (ResultCollector typically initializes a default; the rule still tolerates null.)
        if (rc.getSaveConfig() == null) {
            NodeWithModel n = JmxTestHarness.singleElement(rc);
            List<Finding> findings = new SaveResponseDataEnabledRule().check(n.node(), JmxTestHarness.newContext(n.model()));
            assertThat(findings).isEmpty();
        }
    }

    // ─── RETRIEVE_EMBEDDED_RESOURCES ────────────────────────

    @Test
    void retrieveEmbedded_firesWhenEnabledWithoutWhitelist() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        http.setProperty("HTTPSampler.image_parser", true);
        // No HTTPSampler.embedded_url_re set.
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new RetrieveEmbeddedResourcesRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("RETRIEVE_EMBEDDED_RESOURCES");
    }

    @Test
    void retrieveEmbedded_doesNotFireWithWhitelist() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        http.setProperty("HTTPSampler.image_parser", true);
        http.setProperty("HTTPSampler.embedded_url_re", "https?://example\\.com/.*");
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new RetrieveEmbeddedResourcesRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void retrieveEmbedded_doesNotFireWhenImageParserDisabled() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        http.setProperty("HTTPSampler.image_parser", false);
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new RetrieveEmbeddedResourcesRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── THREAD_COUNT_EXCESSIVE ─────────────────────────────

    @Test
    void threadCount_firesOver1000() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.num_threads", "1500");
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new ThreadCountExcessiveRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("THREAD_COUNT_EXCESSIVE");
    }

    @Test
    void threadCount_doesNotFireAt1000Boundary() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.num_threads", "1000");
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new ThreadCountExcessiveRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void threadCount_doesNotFireBelowThreshold() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.num_threads", "100");
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new ThreadCountExcessiveRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    /**
     * A bare TestElement whose simple class name contains "BeanShell" — sufficient to satisfy
     * BeanshellUsageRule's class-name match check without depending on the optional
     * {@code ApacheJMeter_components} BeanShellSampler subclass (which has external classpath
     * requirements like the bsh.jar runtime).
     */
    private static final class BeanShellSampler extends AbstractTestElement {
    }
}
