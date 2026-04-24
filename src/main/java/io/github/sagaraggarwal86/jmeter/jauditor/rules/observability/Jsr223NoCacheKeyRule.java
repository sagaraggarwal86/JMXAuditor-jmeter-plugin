package io.github.sagaraggarwal86.jmeter.jauditor.rules.observability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class Jsr223NoCacheKeyRule extends AbstractRule {
    @Override
    public String id() {
        return "JSR223_NO_CACHE_KEY";
    }

    @Override
    public Category category() {
        return Category.OBSERVABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String description() {
        return "JSR223 element without compilation cache key — recompiles each iteration.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(TestElement.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        String cls = te.getClass().getName();
        if (!cls.contains("JSR223")) return List.of();
        String key = propString(te, "cacheKey");
        String script = propString(te, "script");
        if (script.isBlank()) return List.of();
        if (!key.isBlank()) return List.of();
        return List.of(make(ctx.pathFor(node),
                "JSR223 script missing cache key",
                "This JSR223 element has a script body but no Cache Key set. Every time the element fires (potentially thousands of times per second under load), Groovy compiles the script from scratch — a process that takes several milliseconds and allocates a lot of short-lived objects. Those milliseconds add up into real latency on top of the actual request, and the allocations pressure the garbage collector, which sometimes kicks in mid-test and creates artificial response-time spikes that look like the system under test misbehaving.",
                "Fill in the Cache Key field with any unique string — 'my_login_script_v1', 'auth_token_builder', anything consistent and distinctive. Groovy uses the key to remember its compiled version of the script, so after the first execution it reuses the cached compile instead of redoing the work. Don't copy-paste the same cache key across multiple elements (that makes the wrong script run); give every JSR223 element its own key, and change the key whenever you edit the script so the cache gets invalidated."));
    }
}
