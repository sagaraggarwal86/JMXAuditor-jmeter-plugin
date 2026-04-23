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

public final class ExtractorNoDefaultRule extends AbstractRule {
    @Override
    public String id() {
        return "EXTRACTOR_NO_DEFAULT";
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
        return "Regex/JSON/Boundary Extractor with empty or missing default value.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(RegexExtractor.class, JSONPostProcessor.class, BoundaryExtractor.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        String defKey = (te instanceof JSONPostProcessor) ? "JSONPostProcessor.defaultValues"
                : (te instanceof BoundaryExtractor) ? "BoundaryExtractor.default_empty_value"
                  : "RegexExtractor.default";
        String altKey = "RegexExtractor.default_empty_value";
        String v1 = propString(te, defKey);
        String v2 = propString(te, altKey);
        if ((v1 == null || v1.isBlank()) && (v2 == null || v2.isBlank())) {
            return List.of(make(ctx.pathFor(node),
                    "Extractor missing default value",
                    "Extractor has no default value set. Extraction failures will silently leave the variable unset.",
                    "Set a sentinel default (e.g., NOT_FOUND) to surface extraction failures in downstream assertions."));
        }
        return List.of();
    }
}
