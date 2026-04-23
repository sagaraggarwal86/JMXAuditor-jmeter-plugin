package io.github.sagaraggarwal86.jmeter.jauditor.rules.realism;

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

public final class MissingRampUpRule extends AbstractRule {
    @Override
    public String id() {
        return "MISSING_RAMP_UP";
    }

    @Override
    public Category category() {
        return Category.REALISM;
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public String description() {
        return "Thread Group with >10 threads and zero ramp-up.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(ThreadGroup.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        int threads = propInt(te, "ThreadGroup.num_threads", 0);
        int ramp = propInt(te, "ThreadGroup.ramp_time", 0);
        if (threads <= 10 || ramp > 0) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Thread Group has no ramp-up",
                "Starting " + threads + " threads instantly creates a thundering herd.",
                "Set ramp-up to 1-10 seconds per 100 threads for a realistic warm-up."));
    }
}
