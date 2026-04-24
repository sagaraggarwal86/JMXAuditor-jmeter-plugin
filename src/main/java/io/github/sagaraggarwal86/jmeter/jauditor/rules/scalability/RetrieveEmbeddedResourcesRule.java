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
        if (!whitelist.isBlank()) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Retrieve Embedded Resources without URL filter",
                "This HTTP sampler has 'Retrieve All Embedded Resources' turned on with no URL filter. That means every image, CSS file, JavaScript file, and iframe source the response references gets fetched automatically — including resources on third-party CDNs, analytics domains, ad networks, and font providers. One main request can turn into fifty actual HTTP calls, and the extra calls pollute the metrics with latencies that have nothing to do with the system you're actually testing.",
                "Set the 'URLs must match' regex field on the sampler to a pattern that whitelists only your own domain — for example, 'https?://([^/]+\\.)?example\\.com/.*' if your app lives at example.com. JMeter will then skip any embedded URL that doesn't match. This keeps the test focused on your infrastructure, makes throughput calculations honest, and avoids accidentally load-testing your CDN provider or third-party tracking scripts."));
    }
}
