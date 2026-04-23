package io.github.sagaraggarwal86.jmeter.jauditor.rules.correctness;

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

public final class ThreadGroupZeroDurationRule extends AbstractRule {
    @Override
    public String id() {
        return "THREAD_GROUP_ZERO_DURATION";
    }

    @Override
    public Category category() {
        return Category.CORRECTNESS;
    }

    @Override
    public Severity severity() {
        return Severity.ERROR;
    }

    @Override
    public String description() {
        return "Thread Group with scheduler enabled but duration = 0 or blank.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(ThreadGroup.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        boolean scheduler = propBool(te, "ThreadGroup.scheduler");
        if (!scheduler) return List.of();
        String dur = propString(te, "ThreadGroup.duration");
        if (dur == null || dur.isBlank() || "0".equals(dur.trim())) {
            return List.of(make(ctx.pathFor(node),
                    "Thread Group scheduler enabled with zero duration",
                    "Scheduler is enabled but duration is blank or zero. Test will stop immediately.",
                    "Set a positive duration, or disable the scheduler."));
        }
        return List.of();
    }
}
