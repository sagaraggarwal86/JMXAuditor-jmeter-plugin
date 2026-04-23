package io.github.sagaraggarwal86.jmeter.jauditor.rules.scalability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
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
        return "HTTP sampler configured to save full response data.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(HTTPSamplerBase.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        boolean save = propBool(te, "HTTPSampler.save_response_as_md5") || propBool(te, "WebServiceSampler.save_response");
        if (!save) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Save Response Data enabled",
                "Saving full response bodies inflates JTL size and can blow out heap under load.",
                "Disable response saving on the load path; enable only for diagnostic runs."));
    }
}
