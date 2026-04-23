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

public final class UnnamedTransactionControllerRule extends AbstractRule {
    @Override
    public String id() {
        return "UNNAMED_TRANSACTION_CONTROLLER";
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
        return "Transaction Controller left with its default name.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(TransactionController.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        String name = node.getName();
        if (name == null) return List.of();
        if (!"Transaction Controller".equals(name.trim())) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Transaction Controller unnamed",
                "Transaction Controller uses the default name, producing unreadable report labels.",
                "Rename to a business-meaningful action (e.g., 'Checkout Flow')."));
    }
}
