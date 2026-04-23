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

public final class RetrieveEmbeddedResourcesRule extends AbstractRule {
    @Override
    public String id() {
        return "RETRIEVE_EMBEDDED_RESOURCES";
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
        return "HTTP sampler retrieves all embedded resources without URL constraints.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(HTTPSamplerBase.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        if (!propBool(te, "HTTPSampler.image_parser")) return List.of();
        String whitelist = propString(te, "HTTPSampler.embedded_url_re");
        if (whitelist != null && !whitelist.isBlank()) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Retrieve Embedded Resources without URL filter",
                "Sampler downloads every asset referenced by the response. This can multiply load unpredictably.",
                "Set an 'URLs must match' regex to scope embedded requests to your application domain."));
    }
}
