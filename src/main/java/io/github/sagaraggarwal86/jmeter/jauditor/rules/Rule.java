package io.github.sagaraggarwal86.jmeter.jauditor.rules;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

/**
 * Contract for the 25 catalogue rules. Implementations are {@code final}, have a
 * package-private no-arg constructor, and are stateless — per-scan state lives in
 * {@link ScanContext}, per-session state in
 * {@code JAuditorSession}, and {@link #check} must be side-effect-free (invariant 11).
 * {@link #appliesTo()} is consulted once per concrete element class per scan; rules
 * targeting the whole tree return {@code Set.of(TestPlan.class)} and use
 * {@link ScanContext#memoize} or {@link ScanContext#hasDescendantOfType} to scan
 * beyond the single {@code TestPlan} node.
 */
public interface Rule {
    String id();

    Category category();

    Severity severity();

    String description();

    Set<Class<? extends TestElement>> appliesTo();

    List<Finding> check(JMeterTreeNode node, ScanContext ctx);
}
