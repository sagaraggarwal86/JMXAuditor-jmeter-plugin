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
        if (te.isEnabled()) return List.of();
        // Skip the root test plan if somehow flagged
        if (node.getParent() == null) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Disabled element in tree",
                "The element '" + node.getName() + "' is disabled — it still exists in the test tree and gets saved into the .jmx file, but it doesn't execute during test runs. Over time, disabled elements pile up: an experiment someone tried once, a listener left over from debugging, a branch commented out 'temporarily' months ago. They confuse anyone reading the test plan later because it's hard to tell whether a disabled element is intentionally paused or forgotten junk.",
                "If the element is genuinely no longer needed, delete it — .jmx files are in version control, so if you ever want it back it's one git log away. If you're keeping it for a specific reason (a debug listener you re-enable when investigating something, an alternative flow that might come back), add a Comment on the element (right-click → Edit Comment) explaining why it's there and what it's for, so the next person understands at a glance."));
    }
}
