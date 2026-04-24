package io.github.sagaraggarwal86.jmeter.jauditor.model;

import org.apache.jmeter.gui.tree.JMeterTreeNode;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete result of a single scan. The compact constructor defensively copies both
 * list fields; see the inline comment on {@code navigation} for why identity-map
 * semantics are preserved there instead of using {@code Map.copyOf}. The
 * {@code navigation} map is {@code @JsonIgnore}'d via a mixin — it would leak live
 * {@code JMeterTreeNode} references into the JSON output.
 */
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
        // IdentityHashMap preserved — Finding is a record with structural equals, so two separate findings that
        // happen to share every field (e.g. two unnamed "JSON Extractor" siblings under the same sampler) compare
        // equal. Map.copyOf would collide them; identity semantics keep them as distinct navigation targets.
        navigation = (navigation == null)
                ? Map.of()
                : Collections.unmodifiableMap(new IdentityHashMap<>(navigation));
    }
}
