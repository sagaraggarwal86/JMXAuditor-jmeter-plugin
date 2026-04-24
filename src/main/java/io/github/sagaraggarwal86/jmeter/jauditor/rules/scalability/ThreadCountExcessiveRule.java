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
                "This Thread Group is set to run " + threads + " virtual users inside a single JVM. A single JMeter process can usually handle 500-1000 threads comfortably; past that, threads compete for CPU time and memory so heavily that they can't actually issue requests at the rate you configured. You end up measuring JMeter's own scheduling delays rather than the system under test, and the reported TPS plateaus well below what the target could actually handle.",
                "Split the load across multiple injectors. Two common approaches: run several Thread Groups of 500-1000 threads each on the same machine if CPU and memory allow (a common sizing heuristic), or distribute the test across multiple JMeter engines using distributed mode (one controller, several workers) or independent instances coordinated externally. As a rule of thumb, keep each injector's CPU below about 70% during the run — past that, JMeter tends to fall behind its own schedule."));
    }
}
