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
        if (dur.isBlank() || "0".equals(dur.trim())) {
            return List.of(make(ctx.pathFor(node),
                    "Thread Group scheduler enabled with zero duration",
                    "This Thread Group has its scheduler switched on but no duration filled in (the field is empty or set to 0). JMeter reads that as 'run for zero seconds' — so the moment the test starts, the scheduler tells the threads they're already out of time and they shut down before any meaningful work happens.",
                    "Pick one of two fixes. If you want a time-boxed run, enter how long the test should last in the Duration field in seconds — for example, 300 for a five-minute run. If you'd rather end the test based on iterations instead of time, turn the scheduler off entirely and let the Loop Count drive when it stops. Leaving the scheduler on with no duration never does what anyone wants."));
        }
        return List.of();
    }
}
