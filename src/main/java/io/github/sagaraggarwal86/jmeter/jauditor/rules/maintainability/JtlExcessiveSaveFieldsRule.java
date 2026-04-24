package io.github.sagaraggarwal86.jmeter.jauditor.rules.maintainability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class JtlExcessiveSaveFieldsRule extends AbstractRule {

    private static final int THRESHOLD = 20;

    @Override
    public String id() {
        return "JTL_EXCESSIVE_SAVE_FIELDS";
    }

    @Override
    public Category category() {
        return Category.MAINTAINABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String description() {
        return "Test Plan enables more than 20 jmeter.save.saveservice.* fields.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(TestPlan.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        if (te == null) return List.of();
        List<String> enabled = new ArrayList<>();
        PropertyIterator it = te.propertyIterator();
        while (it.hasNext()) {
            JMeterProperty p = it.next();
            String key = p.getName();
            if (key == null || !key.startsWith("jmeter.save.saveservice.")) continue;
            if (Boolean.parseBoolean(p.getStringValue())) enabled.add(key);
        }
        if (enabled.size() <= THRESHOLD) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Excessive JTL save fields enabled",
                "This test plan has " + enabled.size() + " jmeter.save.saveservice.* properties set to true, which tells JMeter to write that many columns into every row of the JTL results file. Every extra column adds I/O work during the test and disk space afterwards — on a long run with millions of samples, the difference between a minimal column set and everything enabled can be tens of gigabytes plus noticeably higher CPU overhead in the writer thread, which sometimes ends up slowing the test itself.",
                "Trim the save fields down to the ones you actually use for analysis. One practical minimal set is: timestamp, elapsed, label, responseCode, success, threadName — six columns that together cover throughput, error rate, per-sampler latency, and per-thread grouping. Turn the rest off by removing the corresponding jmeter.save.saveservice.* properties from the Test Plan (or setting them to false). Keep the richer set only for targeted diagnostic runs where you specifically need response times by sub-component, assertion results, or latency breakdowns."));
    }
}
