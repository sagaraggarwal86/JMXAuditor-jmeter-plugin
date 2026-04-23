package io.github.sagaraggarwal86.jmeter.jauditor.rules.maintainability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class CsvAbsolutePathRule extends AbstractRule {
    @Override
    public String id() {
        return "CSV_ABSOLUTE_PATH";
    }

    @Override
    public Category category() {
        return Category.MAINTAINABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String description() {
        return "CSV Data Set Config references an absolute file path.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(CSVDataSet.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        String fn = propString(node.getTestElement(), "filename");
        if (fn == null || fn.isBlank() || hasJMeterVar(fn)) return List.of();
        boolean absolute = fn.startsWith("/") || (fn.length() > 2 && fn.charAt(1) == ':');
        if (!absolute) return List.of();
        return List.of(make(ctx.pathFor(node),
                "CSV Data Set uses absolute path",
                "File path '" + fn + "' is absolute. It will not resolve on other machines.",
                "Use a path relative to the .jmx location, or parameterize via ${CSV_DIR}."));
    }
}
