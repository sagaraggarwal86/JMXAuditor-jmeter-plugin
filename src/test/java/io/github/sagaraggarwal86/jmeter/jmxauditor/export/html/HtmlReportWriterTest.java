package io.github.sagaraggarwal86.jmeter.jmxauditor.export.html;

import io.github.sagaraggarwal86.jmeter.jmxauditor.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlReportWriterTest {

    @Test
    void emptyScanProducesCleanReport(@TempDir Path tmp) throws IOException {
        ScanResult r = new ScanResult(Instant.EPOCH, null, "empty.jmx", "5.6.3", false,
                1, 25, 0, List.of(), ScanOutcome.COMPLETE, List.of(), null);
        Path out = tmp.resolve("report.html");
        HtmlReportWriter.write(r, "0.2.0", out);

        String html = Files.readString(out);
        assertThat(html).contains("JMXAuditor Report");
        assertThat(html).contains("empty.jmx");
        assertThat(html).contains("No findings. Script looks clean.");
        // Rule reference renders 25 rule rows.
        assertThat(html).contains("Rule reference (all 25 rules)");
        // No category nav-count spans should appear (the literal "nav-count" text
        // shows up in the inlined CSS, but no findings means no per-category badge spans).
        assertThat(html).doesNotContain("<span class=\"nav-count\">");
    }

    @Test
    void truncatedOutcomeRendersInfoBanner(@TempDir Path tmp) throws IOException {
        ScanResult r = new ScanResult(Instant.EPOCH, "/x.jmx", "x.jmx", "5.6.3", true,
                10000, 25, 9999, List.of(), ScanOutcome.NODE_LIMIT, List.of(), null);
        Path out = tmp.resolve("report.html");
        HtmlReportWriter.write(r, "0.2.0", out);

        String html = Files.readString(out);
        assertThat(html).contains("banner-warn"); // unsavedChanges banner
        assertThat(html).contains("banner-info"); // truncation banner
        assertThat(html).contains("10000-node cap");
    }

    @Test
    void groupsFindingsBeyondThreshold(@TempDir Path tmp) throws IOException {
        // GROUP_THRESHOLD = 3 — five findings of the same rule id collapse to head + 4 hidden members.
        List<Finding> many = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            many.add(new Finding("HARDCODED_HOST", Category.MAINTAINABILITY, Severity.WARN,
                    "Hard-coded hostname", "desc", "fix",
                    new NodePath(List.of("Test Plan", "TG", "HTTP " + i)), 3));
        }
        ScanResult r = new ScanResult(Instant.EPOCH, null, "x.jmx", "5.6.3", false,
                10, 25, 0, many, ScanOutcome.COMPLETE, List.of(), null);
        Path out = tmp.resolve("report.html");
        HtmlReportWriter.write(r, "0.2.0", out);

        String html = Files.readString(out);
        assertThat(html).contains("class=\"grp-head\"");
        assertThat(html).contains("class=\"grp-member\" hidden");
        // Per-rule expand toggle and "+N more" affordance render on the head row.
        assertThat(html).contains("grp-toggle");
        assertThat(html).contains("grp-path-more");
        assertThat(html).contains("data-more=\"4\""); // 5 - 1
    }

    @Test
    void escapesUserSuppliedJmxFileName(@TempDir Path tmp) throws IOException {
        ScanResult r = new ScanResult(Instant.EPOCH, null, "<script>x.jmx", "5.6.3", false,
                1, 25, 0, List.of(), ScanOutcome.COMPLETE, List.of(), null);
        Path out = tmp.resolve("report.html");
        HtmlReportWriter.write(r, "0.2.0", out);

        String html = Files.readString(out);
        assertThat(html).contains("&lt;script&gt;x.jmx");
        assertThat(html).doesNotContain("<script>x.jmx");
    }

    @Test
    void unsavedJmxNameFallback(@TempDir Path tmp) throws IOException {
        ScanResult r = new ScanResult(Instant.EPOCH, null, null, "5.6.3", false,
                0, 25, 0, List.of(), ScanOutcome.COMPLETE, List.of(), null);
        Path out = tmp.resolve("report.html");
        HtmlReportWriter.write(r, "0.2.0", out);

        String html = Files.readString(out);
        assertThat(html).contains("(unsaved test plan)");
        assertThat(html).contains("untitled");
    }

    @Test
    void categoryPanelRendersWhenFindingsPresent(@TempDir Path tmp) throws IOException {
        Finding f = new Finding("HARDCODED_HOST", Category.MAINTAINABILITY, Severity.WARN,
                "Title", "Desc", "Sug",
                new NodePath(List.of("Test Plan", "Sampler")), 2);
        ScanResult r = new ScanResult(Instant.EPOCH, null, "x.jmx", "5.6.3", false,
                1, 25, 0, List.of(f), ScanOutcome.COMPLETE, List.of(), null);
        Path out = tmp.resolve("report.html");
        HtmlReportWriter.write(r, "0.2.0", out);

        String html = Files.readString(out);
        assertThat(html).contains("panel-maintainability");
        assertThat(html).contains("nav-count"); // category nav button rendered
        assertThat(html).contains("Sampler"); // breadcrumb in node-path cell
    }
}
