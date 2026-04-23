package io.github.sagaraggarwal86.jmeter.jauditor.rules.scalability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.ThreadGroup;

import java.util.List;
import java.util.Set;

public final class ThreadCountExcessiveRule extends AbstractRule {
    @Override
    public String id() {
        return "THREAD_COUNT_EXCESSIVE";
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
        return "Single Thread Group configured with more than 1000 threads.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(ThreadGroup.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        int threads = propInt(node.getTestElement(), "ThreadGroup.num_threads", 0);
        if (threads <= 1000) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Thread Group has >1000 threads",
                "Single Thread Group is configured with " + threads + " threads. Per-JVM limits make this unreliable.",
                "Split across multiple Thread Groups or inject from multiple engines."));
    }
}
