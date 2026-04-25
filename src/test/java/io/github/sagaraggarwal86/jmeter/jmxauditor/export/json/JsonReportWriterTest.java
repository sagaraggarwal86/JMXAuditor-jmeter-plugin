package io.github.sagaraggarwal86.jmeter.jmxauditor.export.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonReportWriterTest {

    @Test
    void writesSchema10WithFindingsAndRules(@TempDir Path tmp) throws IOException {
        Finding f = new Finding("HARDCODED_HOST", Category.MAINTAINABILITY, Severity.WARN,
                "Hard-coded hostname",
                "Server Name field is set to api.example.com",
                "Replace literal hostname with ${HOST}",
                new NodePath(List.of("Test Plan", "Thread Group", "HTTP Request")), 3);
        ScanResult r = new ScanResult(Instant.parse("2026-04-24T10:00:00Z"),
                "/plans/checkout.jmx", "checkout.jmx", "5.6.3", true,
                42, 25, 137, List.of(f), ScanOutcome.COMPLETE, List.of("BEANSHELL_USAGE"), null);

        Path out = tmp.resolve("report.json");
        JsonReportWriter.write(r, "0.2.0", out);

        ObjectMapper m = new ObjectMapper();
        JsonNode root = m.readTree(Files.readString(out));
        assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");
        assertThat(root.get("plugin").get("name").asText()).isEqualTo("JMXAuditor");
        assertThat(root.get("plugin").get("version").asText()).isEqualTo("0.2.0");

        JsonNode scan = root.get("scan");
        assertThat(scan.get("jmxFile").asText()).isEqualTo("/plans/checkout.jmx");
        assertThat(scan.get("jmxFileName").asText()).isEqualTo("checkout.jmx");
        assertThat(scan.get("unsavedChanges").asBoolean()).isTrue();
        assertThat(scan.get("nodesAnalyzed").asInt()).isEqualTo(42);
        assertThat(scan.get("rulesExecuted").asInt()).isEqualTo(25);
        assertThat(scan.get("durationMs").asLong()).isEqualTo(137);
        assertThat(scan.get("outcome").asText()).isEqualTo("complete");
        assertThat(scan.get("timestamp").asText()).isEqualTo("2026-04-24T10:00:00Z");

        JsonNode summary = root.get("summary");
        assertThat(summary.get("totalFindings").asInt()).isEqualTo(1);
        assertThat(summary.get("severityCounts").get("warn").asInt()).isEqualTo(1);
        assertThat(summary.get("categoryCounts").get("maintainability").asInt()).isEqualTo(1);

        assertThat(root.get("findings")).hasSize(1);
        assertThat(root.get("findings").get(0).get("ruleId").asText()).isEqualTo("HARDCODED_HOST");
        // Navigation must NOT appear in JSON (invariant 1: navigation Jackson-ignored via mixin).
        assertThat(root.get("findings").get(0).has("navigation")).isFalse();

        JsonNode rules = root.get("rules");
        assertThat(rules.size()).isEqualTo(25);
        boolean foundSuppressed = false;
        for (JsonNode rn : rules) {
            if ("BEANSHELL_USAGE".equals(rn.get("id").asText())) {
                assertThat(rn.get("suppressed").asBoolean()).isTrue();
                foundSuppressed = true;
            }
        }
        assertThat(foundSuppressed).isTrue();
    }

    @Test
    void omitsJmxFileWhenNull(@TempDir Path tmp) throws IOException {
        ScanResult r = new ScanResult(Instant.EPOCH, null, "unsaved.jmx", "5.6.3", false,
                0, 25, 0, List.of(), ScanOutcome.COMPLETE, List.of(), null);
        Path out = tmp.resolve("report.json");
        JsonReportWriter.write(r, "0.2.0", out);
        JsonNode root = new ObjectMapper().readTree(Files.readString(out));
        assertThat(root.get("scan").has("jmxFile")).isFalse();
    }
}
