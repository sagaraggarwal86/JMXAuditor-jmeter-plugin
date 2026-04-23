package io.github.sagaraggarwal86.jmeter.jauditor.rules.maintainability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.ThreadGroup;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public final class MissingTransactionControllerRule extends AbstractRule {
    @Override
    public String id() {
        return "MISSING_TRANSACTION_CONTROLLER";
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
        return "Thread Group has samplers not wrapped in Transaction Controllers.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(ThreadGroup.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        List<JMeterTreeNode> loose = new ArrayList<>();
        Enumeration<?> en = node.children();
        while (en.hasMoreElements()) {
            Object c = en.nextElement();
            if (c instanceof JMeterTreeNode tn) {
                TestElement te = tn.getTestElement();
                if (te instanceof Sampler) loose.add(tn);
            }
        }
        if (loose.isEmpty()) return List.of();
        // If user wraps samplers in Transaction Controllers elsewhere, direct-under-TG samplers are still unwrapped.
        // We still fire — the issue is the loose samplers, not absence of any TC anywhere.
        return List.of(make(ctx.pathFor(node),
                "Samplers outside Transaction Controllers",
                loose.size() + " sampler(s) at the Thread Group level are not wrapped in Transaction Controllers.",
                "Wrap related samplers in a Transaction Controller with a business-meaningful name."));
    }
}
