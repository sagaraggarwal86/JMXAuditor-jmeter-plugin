package io.github.sagaraggarwal86.jmeter.jauditor.rules.observability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.assertions.Assertion;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.testelement.TestElement;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public final class HttpSamplerNoAssertionRule extends AbstractRule {
    private static boolean hasAssertionIn(JMeterTreeNode node) {
        Enumeration<?> en = node.children();
        while (en.hasMoreElements()) {
            Object c = en.nextElement();
            if (c instanceof JMeterTreeNode tn && tn.getTestElement() instanceof Assertion) return true;
        }
        return false;
    }

    @Override
    public String id() {
        return "HTTP_SAMPLER_NO_ASSERTION";
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
        return "HTTP Sampler without a Response Assertion at element or parent scope.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(HTTPSamplerBase.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        if (hasAssertionIn(node)) return List.of();
        JMeterTreeNode parent = (JMeterTreeNode) node.getParent();
        while (parent != null) {
            if (hasAssertionIn(parent)) return List.of();
            parent = (JMeterTreeNode) parent.getParent();
        }
        return List.of(make(ctx.pathFor(node),
                "HTTP Sampler has no Response Assertion",
                "Sampler has no Response Assertion in scope. HTTP 200 with an error body will be counted as success.",
                "Add a Response Assertion on status code or response content — or rely on a parent-scope assertion."));
    }
}
