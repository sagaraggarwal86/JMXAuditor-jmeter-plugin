package io.github.sagaraggarwal86.jmeter.jauditor.export.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sagaraggarwal86.jmeter.jauditor.export.ReportAggregates;
import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the schema 1.0 JSON report: pretty-printed UTF-8, camelCase keys, lowercase
 * enum values, {@code NON_NULL} omission, ISO-8601 timestamps with zone offset.
 * The schema is part of the public API contract — renames or removals bump
 * {@code schemaVersion} (invariant 1).
 */
public final class JsonReportWriter {

    private static final ObjectMapper MAPPER = JAuditorObjectMapper.create();

    private JsonReportWriter() {
    }

    public static void write(ScanResult r, String pluginVersion, Path out) throws IOException {
        ReportAggregates agg = ReportAggregates.of(r);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", "1.0");
        root.put("plugin", Map.of("name", "JAuditor", "version", pluginVersion));

        Map<String, Object> scan = new LinkedHashMap<>();
        scan.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(r.scanTimestamp()));
        if (r.jmxFilePath() != null) scan.put("jmxFile", r.jmxFilePath());
        scan.put("jmxFileName", r.jmxFileName());
        scan.put("jmeterVersion", r.jmeterVersion());
        scan.put("unsavedChanges", r.unsavedChanges());
        scan.put("nodesAnalyzed", r.nodesAnalyzed());
        scan.put("rulesExecuted", r.rulesExecuted());
        scan.put("durationMs", r.durationMs());
        scan.put("outcome", r.outcome().asJson());
        root.put("scan", scan);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalFindings", r.findings().size());
        Map<String, Integer> sevCounts = new LinkedHashMap<>();
        agg.bySeverity().forEach((k, v) -> sevCounts.put(k.asJson(), v));
        summary.put("severityCounts", sevCounts);
        Map<String, Integer> catCounts = new LinkedHashMap<>();
        agg.byCategory().forEach((k, v) -> catCounts.put(k.asJson(), v));
        summary.put("categoryCounts", catCounts);
        root.put("summary", summary);

        root.put("findings", r.findings());

        List<Map<String, Object>> rules = new ArrayList<>();
        for (ReportAggregates.RuleRow row : agg.rules()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", row.id());
            entry.put("category", row.category().asJson());
            entry.put("defaultSeverity", row.severity().asJson());
            entry.put("description", row.description());
            entry.put("suppressed", row.suppressed());
            rules.add(entry);
        }
        root.put("rules", rules);

        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        Files.writeString(out, json, StandardCharsets.UTF_8);
    }
}
