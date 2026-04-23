package io.github.sagaraggarwal86.jmeter.jauditor.rules;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public interface Rule {
    String id();

    Category category();

    Severity severity();

    String description();

    Set<Class<? extends TestElement>> appliesTo();

    List<Finding> check(JMeterTreeNode node, ScanContext ctx);
}
