package io.github.sagaraggarwal86.jmeter.jauditor.rules.maintainability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class DisabledElementInTreeRule extends AbstractRule {
    @Override
    public String id() {
        return "DISABLED_ELEMENT_IN_TREE";
    }

    @Override
    public Category category() {
        return Category.MAINTAINABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public String description() {
        return "Disabled element left in the tree.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(TestElement.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        if (te == null || te.isEnabled()) return List.of();
        // Skip the root test plan if somehow flagged
        if (node.getParent() == null) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Disabled element in tree",
                "Element '" + node.getName() + "' is disabled but remains in the test plan.",
                "Remove dead branches, or add a comment explaining why it's retained."));
    }
}
