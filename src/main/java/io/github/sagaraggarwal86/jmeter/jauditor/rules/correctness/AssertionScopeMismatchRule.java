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
        // Fire only for blank or "parent" (Main sample only). "all" already covers sub-samples,
        // "children" scopes to sub-samples, "variable" is user-defined — none of those mismatch image_parser.
        if (scope != null && !scope.isBlank() && !"parent".equalsIgnoreCase(scope)) {
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
                        "This Response Assertion is set to check only the main sample, but its parent HTTP sampler has 'Retrieve All Embedded Resources' turned on — which means every image, CSS file, and JS file the page pulls in becomes its own sub-sample. If any of those sub-samples fails (a broken image, a 404 on a stylesheet), the assertion can't see it, because it only ever looks at the main HTML response. The test reports success even when half the page didn't load.",
                        "Open the Response Assertion and change the scope dropdown from 'Main sample only' (or blank, which means the same thing) to 'Main sample and sub-samples'. After the change, the assertion will evaluate the main page and every embedded resource, so a broken sub-request shows up as a test failure. If you genuinely only care about the main response — say, you're asserting HTML content and don't care about asset availability — leave the scope alone and disable this check for that sampler."));
            }
        }
        return List.of();
    }
}
