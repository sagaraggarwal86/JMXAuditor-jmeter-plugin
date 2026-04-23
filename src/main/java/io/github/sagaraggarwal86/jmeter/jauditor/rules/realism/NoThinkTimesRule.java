package io.github.sagaraggarwal86.jmeter.jauditor.rules.realism;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.timers.Timer;

import java.util.List;
import java.util.Set;

public final class NoThinkTimesRule extends AbstractRule {
    @Override
    public String id() {
        return "NO_THINK_TIMES";
    }

    @Override
    public Category category() {
        return Category.REALISM;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String description() {
        return "Thread Group contains samplers but no Timer in its subtree.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(ThreadGroup.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        if (!ctx.hasDescendantOfType(node, Sampler.class)) return List.of();
        if (ctx.hasDescendantOfType(node, Timer.class)) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Thread Group has no think times",
                "Samplers fire back-to-back with no Timer. Users don't behave that way.",
                "Add a Constant Timer or Gaussian Random Timer to simulate realistic pacing."));
    }
}
