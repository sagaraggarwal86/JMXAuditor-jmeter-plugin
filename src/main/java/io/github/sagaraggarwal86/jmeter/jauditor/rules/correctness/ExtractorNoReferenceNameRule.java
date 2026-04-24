package io.github.sagaraggarwal86.jmeter.jauditor.rules.correctness;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.extractor.BoundaryExtractor;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class ExtractorNoReferenceNameRule extends AbstractRule {
    @Override
    public String id() {
        return "EXTRACTOR_NO_REFERENCE_NAME";
    }

    @Override
    public Category category() {
        return Category.CORRECTNESS;
    }

    @Override
    public Severity severity() {
        return Severity.ERROR;
    }

    @Override
    public String description() {
        return "Extractor element with empty Reference Name field.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(RegexExtractor.class, JSONPostProcessor.class, BoundaryExtractor.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        String refKey = (te instanceof JSONPostProcessor) ? "JSONPostProcessor.referenceNames"
                : (te instanceof BoundaryExtractor) ? "BoundaryExtractor.refname"
                  : "RegexExtractor.refname";
        String v = propString(te, refKey);
        if (v.isBlank()) {
            return List.of(make(ctx.pathFor(node),
                    "Extractor missing reference name",
                    "This extractor runs its extraction logic but has no reference name set, so whatever it pulls out of the response goes nowhere — there's no JMeter variable for later samplers, assertions, or scripts to read it from. Effectively the extractor is doing work that produces no usable output, and any downstream element that was expecting a variable will see it as undefined.",
                    "Set the Reference Name field on the extractor to the variable name you want to use downstream — for example, authToken if a later sampler needs ${authToken} in its header. Pick a name that makes the value's purpose obvious at a glance, and make sure it matches exactly what the rest of the test plan references (JMeter variable names are case-sensitive)."));
        }
        return List.of();
    }
}
