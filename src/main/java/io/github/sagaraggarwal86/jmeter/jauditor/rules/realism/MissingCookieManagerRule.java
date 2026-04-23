package io.github.sagaraggarwal86.jmeter.jauditor.rules.realism;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;

import java.util.List;
import java.util.Set;

public final class MissingCookieManagerRule extends AbstractRule {
    @Override
    public String id() {
        return "MISSING_COOKIE_MANAGER";
    }

    @Override
    public Category category() {
        return Category.REALISM;
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public String description() {
        return "Test plan has HTTP samplers but no HTTP Cookie Manager.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(TestPlan.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        boolean hasHttp = ctx.memoize("anyHttpSampler",
                () -> allNodes(ctx.tree()).stream().anyMatch(n -> n.getTestElement() instanceof HTTPSamplerBase));
        if (!hasHttp) return List.of();
        boolean hasCookie = ctx.memoize("anyCookieManager",
                () -> allNodes(ctx.tree()).stream().anyMatch(n -> n.getTestElement() instanceof CookieManager));
        if (hasCookie) return List.of();
        return List.of(make(ctx.pathFor(node),
                "No HTTP Cookie Manager",
                "Test plan has HTTP samplers but no Cookie Manager. Session-based apps won't authenticate correctly.",
                "Add an HTTP Cookie Manager at the Test Plan or Thread Group level."));
    }
}
