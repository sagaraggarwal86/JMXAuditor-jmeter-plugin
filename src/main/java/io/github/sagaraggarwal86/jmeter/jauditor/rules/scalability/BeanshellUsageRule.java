package io.github.sagaraggarwal86.jmeter.jauditor.rules.scalability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class BeanshellUsageRule extends AbstractRule {
    @Override
    public String id() {
        return "BEANSHELL_USAGE";
    }

    @Override
    public Category category() {
        return Category.SCALABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String description() {
        return "BeanShell Sampler/PreProcessor/PostProcessor/Assertion/Listener in use.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(TestElement.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        String cls = te.getClass().getName();
        if (!cls.contains("beanshell") && !cls.contains("BeanShell")) return List.of();
        return List.of(make(ctx.pathFor(node),
                "BeanShell element in use",
                "BeanShell is slow and not thread-safe under load.",
                "Replace with a JSR223 element using the Groovy language engine."));
    }
}
