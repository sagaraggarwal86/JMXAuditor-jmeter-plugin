package io.github.sagaraggarwal86.jmeter.jauditor.rules.correctness;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class AssertionScopeMismatchRule extends AbstractRule {
    @Override
    public String id() {
        return "ASSERTION_SCOPE_MISMATCH";
    }

    @Override
    public Category category() {
        return Category.CORRECTNESS;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String description() {
        return "Response Assertion with 'Main sample only' scope on sampler with sub-samples.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(ResponseAssertion.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        String scope = propString(node.getTestElement(), "Assertion.scope");
        if (scope != null && !scope.isBlank() && !"parent".equalsIgnoreCase(scope) && !"all".equalsIgnoreCase(scope)) {
            return List.of();
        }
        JMeterTreeNode parent = (JMeterTreeNode) node.getParent();
        if (parent == null) return List.of();
        TestElement pte = parent.getTestElement();
        if (!(pte instanceof Sampler)) return List.of();
        if (pte instanceof HTTPSamplerBase http) {
            boolean embedded = propBool(http, "HTTPSampler.image_parser");
            if (embedded) {
                return List.of(make(ctx.pathFor(node),
                        "Assertion scope may miss sub-samples",
                        "Response Assertion uses default/main-sample scope on a sampler that generates sub-samples (embedded resources).",
                        "Set assertion scope to 'Main sample and sub-samples' if sub-sample validation is required."));
            }
        }
        return List.of();
    }
}
