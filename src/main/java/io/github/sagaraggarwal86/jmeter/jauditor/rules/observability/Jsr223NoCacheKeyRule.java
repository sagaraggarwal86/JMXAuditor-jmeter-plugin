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
        if (script == null || script.isBlank()) return List.of();
        if (key != null && !key.isBlank()) return List.of();
        return List.of(make(ctx.pathFor(node),
                "JSR223 script missing cache key",
                "Without a cache key, Groovy recompiles the script on every execution — high CPU cost under load.",
                "Set a unique cacheKey (e.g., 'my_script_v1') on each JSR223 element."));
    }
}
