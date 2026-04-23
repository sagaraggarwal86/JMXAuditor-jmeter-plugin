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
        return "Test Plan enables a large number of jmeter.save.saveservice.* fields.";
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
                enabled.size() + " jmeter.save.saveservice.* fields are enabled. JTL files will bloat.",
                "Disable fields not needed for analysis — keep timestamp, elapsed, label, responseCode, success, threadName."));
    }
}
