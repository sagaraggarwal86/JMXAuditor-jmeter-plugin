package io.github.sagaraggarwal86.jmeter.jauditor.model;

import org.apache.jmeter.gui.tree.JMeterTreeNode;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ScanResult(
        Instant scanTimestamp,
        String jmxFilePath,
        String jmxFileName,
        String jmeterVersion,
        boolean unsavedChanges,
        int nodesAnalyzed,
        int rulesExecuted,
        long durationMs,
        List<Finding> findings,
        ScanOutcome outcome,
        List<String> suppressedRuleIds,
        Map<Finding, WeakReference<JMeterTreeNode>> navigation
) {
    public ScanResult {
        findings = List.copyOf(findings);
        suppressedRuleIds = List.copyOf(suppressedRuleIds);
        navigation = (navigation == null) ? Map.of() : Map.copyOf(navigation);
    }
}
