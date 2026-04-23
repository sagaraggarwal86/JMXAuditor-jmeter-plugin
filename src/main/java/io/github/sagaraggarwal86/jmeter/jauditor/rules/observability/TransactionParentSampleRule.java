package io.github.sagaraggarwal86.jmeter.jauditor.rules.observability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class TransactionParentSampleRule extends AbstractRule {
    @Override
    public String id() {
        return "TRANSACTION_PARENT_SAMPLE";
    }

    @Override
    public Category category() {
        return Category.OBSERVABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public String description() {
        return "Transaction Controller without 'Generate Parent Sample' enabled.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(TransactionController.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        boolean parent = propBool(node.getTestElement(), "TransactionController.parent");
        if (parent) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Transaction Controller not generating parent sample",
                "Transaction totals won't appear as a single row in aggregate reports.",
                "Enable 'Generate Parent Sample' for cleaner summary metrics."));
    }
}
