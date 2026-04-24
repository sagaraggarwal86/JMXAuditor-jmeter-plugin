package io.github.sagaraggarwal86.jmeter.jauditor.model;

import java.util.Objects;

/**
 * One finding emitted by a rule. Immutable record with defensive null-checks in the
 * compact constructor. Never holds a {@code JMeterTreeNode} reference directly —
 * navigation is resolved through {@link ScanResult#navigation()} so the tree can be
 * garbage-collected without leaking.
 */
public record Finding(
        String ruleId,
        Category category,
        Severity severity,
        String title,
        String description,
        String suggestion,
        NodePath nodePath,
        int treeDepth
) {
    public Finding {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(nodePath, "nodePath");
    }

    /**
     * Synthetic INFO finding appended when a rule throws at Tier 1 (see CLAUDE.md
     * "Exception topology"). Surfacing the failure to the user is deliberate — a silent
     * rule crash would let the plan look clean when it wasn't actually audited.
     */
    public static Finding ruleFailure(String ruleId, Category category, NodePath path, Throwable cause) {
        return new Finding(
                ruleId,
                category,
                Severity.INFO,
                "Rule execution failed",
                "Rule " + ruleId + " threw: " + cause.getClass().getSimpleName(),
                "This rule was skipped. See jmeter.log for details.",
                path,
                path.depth()
        );
    }
}
