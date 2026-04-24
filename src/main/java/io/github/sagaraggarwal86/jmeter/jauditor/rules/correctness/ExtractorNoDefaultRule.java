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
                : (te instanceof BoundaryExtractor) ? "BoundaryExtractor.default"
                  : "RegexExtractor.default";
        if (!propBlank(te, defKey)) return List.of();
        // "default_empty_value" is a boolean flag — "assign empty string on no-match" — not a string value.
        if (te instanceof RegexExtractor && propBool(te, "RegexExtractor.default_empty_value")) return List.of();
        if (te instanceof BoundaryExtractor && propBool(te, "BoundaryExtractor.default_empty_value")) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Extractor missing default value",
                "This extractor (Regex, JSON, or Boundary) has no default value configured. If the response ever doesn't match what the extractor is looking for — a different error page, a redirect, an empty body — the variable it was supposed to set just never gets assigned. Downstream samplers and assertions that rely on that variable won't fail loudly; they'll silently use a stale value from a previous iteration or an empty string, and the real bug becomes nearly impossible to spot.",
                "Fill in the Default Value field on the extractor with a sentinel string that obviously doesn't look like real data — something like NOT_FOUND or EXTRACTION_FAILED. Then add a Response Assertion a little further down that fails when the variable equals that sentinel. That way a missed extraction turns into a clear failing sample in the report instead of a silent corruption that you only notice days later when the numbers don't add up."));
    }
}
