package io.github.sagaraggarwal86.jmeter.jauditor.rules.scalability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class SaveResponseDataEnabledRule extends AbstractRule {
    @Override
    public String id() {
        return "SAVE_RESPONSE_DATA_ENABLED";
    }

    @Override
    public Category category() {
        return Category.SCALABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String description() {
        return "Listener configured to save full response bodies into JTL output.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(ResultCollector.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        ResultCollector rc = (ResultCollector) te;
        SampleSaveConfiguration cfg = rc.getSaveConfig();
        if (cfg == null || !cfg.saveResponseData()) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Listener saves full response data",
                "This listener is configured to save the full response body of every sample into its JTL output. Each response is potentially hundreds of kilobytes; on a sustained run the JTL file grows by gigabytes per minute, and JMeter buffers chunks of that in memory along the way. Disk fills up, heap pressure spikes, and the extra I/O slows the actual test down to where the reported response times aren't even representative of the system under test anymore.",
                "Turn off the 'Save Response Data (XML)' checkbox on the listener's Configure panel unless you specifically need the body for later inspection. If you only need bodies for failed requests (a reasonable debugging compromise), set the global property jmeter.save.saveservice.response_data.on_error=true in jmeter.properties — JMeter will then save bodies only when a sample fails. For full-body captures, run a targeted smoke test with a handful of iterations rather than saving every response on a 10,000-thread run."));
    }
}
