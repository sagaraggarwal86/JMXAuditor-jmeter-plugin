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
        if (fn.isBlank() || hasJMeterVar(fn)) return List.of();
        boolean absolute = fn.startsWith("/") || (fn.length() > 2 && fn.charAt(1) == ':');
        if (!absolute) return List.of();
        return List.of(make(ctx.pathFor(node),
                "CSV Data Set uses absolute path",
                "The CSV Data Set is configured to load data from '" + fn + "' — an absolute file path pointing to a specific location on the machine that authored the test plan. Anyone else running the test (a teammate, a CI server, a different engineer) won't have the same directory structure, so the CSV load fails and the test either errors out immediately or silently reuses stale values, depending on how the rest of the test plan is configured.",
                "Change the filename to a path relative to the .jmx file — for example, if the CSV sits next to the test plan, just put 'data/users.csv'. JMeter resolves relative paths against the .jmx's directory, so the test becomes portable. If the CSV lives somewhere conventional but external, use a variable: set ${CSV_DIR} in a User Defined Variables block or a JMeter property, and reference it as ${CSV_DIR}/users.csv. Then each environment can point at its own data directory without editing the test plan."));
    }
}
