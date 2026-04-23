package io.github.sagaraggarwal86.jmeter.jauditor.export.html;

import io.github.sagaraggarwal86.jmeter.jauditor.export.ReportAggregates;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.ui.theme.ThemeColors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HtmlReportWriter {

    private HtmlReportWriter() {
    }

    public static void write(ScanResult r, String pluginVersion, Path out) throws IOException {
        ReportAggregates agg = ReportAggregates.of(r);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("title", "JAuditor Report — " + HtmlEscaper.escape(r.jmxFileName() == null ? "untitled" : r.jmxFileName()));
        tokens.put("jmxFileName", HtmlEscaper.escape(r.jmxFileName() == null ? "(unsaved test plan)" : r.jmxFileName()));
        tokens.put("jmxFilePath", HtmlEscaper.escape(r.jmxFilePath() == null ? "(unsaved)" : r.jmxFilePath()));
        tokens.put("jmeterVersion", HtmlEscaper.escape(r.jmeterVersion() == null ? "" : r.jmeterVersion()));
        tokens.put("scanTimestamp", DateTimeFormatter.ISO_INSTANT.format(r.scanTimestamp()));
        tokens.put("pluginVersion", HtmlEscaper.escape(pluginVersion));
        tokens.put("unsavedBanner", r.unsavedChanges()
                ? "<div class=\"banner banner-warn\">⚠ Report generated from unsaved changes.</div>" : "");
        tokens.put("truncationBanner", r.outcome().isTruncated()
                ? "<div class=\"banner banner-info\">" + HtmlEscaper.escape(r.outcome().bannerMessage(r.findings().size())) + "</div>" : "");
        tokens.put("categoryCards", renderCards(agg));
        tokens.put("findingsByCategory", renderFindings(agg, r.findings().isEmpty()));
        tokens.put("ruleAppendix", renderAppendix(agg));

        String html = HtmlTemplate.render(tokens);
        Files.writeString(out, html, StandardCharsets.UTF_8);
    }

    private static String renderCards(ReportAggregates agg) {
        StringBuilder sb = new StringBuilder("<div class=\"cards\">");
        for (Category c : Category.values()) {
            sb.append("<div class=\"card\" style=\"border-color:").append(ThemeColors.cssHexLight(c)).append("\">")
                    .append("<div class=\"card-name\">").append(cap(c.name())).append("</div>")
                    .append("<div class=\"card-count\">").append(agg.byCategory().get(c)).append("</div>")
                    .append("</div>");
        }
        return sb.append("</div>").toString();
    }

    private static String renderFindings(ReportAggregates agg, boolean noFindings) {
        StringBuilder sb = new StringBuilder();
        for (Category c : Category.values()) {
            List<Finding> list = agg.findingsByCategory().get(c);
            if (list.isEmpty()) continue;
            sb.append("<section class=\"category\" data-category=\"").append(c.name().toLowerCase()).append("\">");
            sb.append("<h2 style=\"color:").append(ThemeColors.cssHexLight(c)).append("\">").append(cap(c.name()))
                    .append(" <span class=\"muted\">(").append(list.size()).append(")</span></h2>");
            sb.append("<table class=\"findings sortable\"><thead><tr>")
                    .append("<th>Severity</th><th>Title</th><th>Node path</th><th>Rule ID</th></tr></thead><tbody>");
            for (Finding f : list) {
                sb.append("<tr>")
                        .append("<td><span class=\"badge ").append(f.severity().asJson()).append("\">").append(severityBadge(f.severity())).append("</span></td>")
                        .append("<td><strong>").append(HtmlEscaper.escape(f.title())).append("</strong><div class=\"desc\">").append(HtmlEscaper.escape(f.description() == null ? "" : f.description())).append("</div>")
                        .append("<div class=\"sugg\">").append(HtmlEscaper.escape(f.suggestion() == null ? "" : f.suggestion())).append("</div></td>")
                        .append("<td><code>").append(HtmlEscaper.escape(f.nodePath().breadcrumb())).append("</code></td>")
                        .append("<td class=\"muted\">").append(HtmlEscaper.escape(f.ruleId())).append("</td>")
                        .append("</tr>");
            }
            sb.append("</tbody></table></section>");
        }
        if (noFindings) sb.append("<div class=\"empty\">✔ No findings. Script looks clean.</div>");
        return sb.toString();
    }

    private static String renderAppendix(ReportAggregates agg) {
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"appendix\"><h2>Rule reference</h2><table><thead><tr>")
                .append("<th>ID</th><th>Category</th><th>Severity</th><th>Description</th><th>Status</th></tr></thead><tbody>");
        for (ReportAggregates.RuleRow row : agg.rules()) {
            sb.append("<tr><td><code>").append(HtmlEscaper.escape(row.id())).append("</code></td>")
                    .append("<td>").append(cap(row.category().name())).append("</td>")
                    .append("<td>").append(row.severity().asJson()).append("</td>")
                    .append("<td>").append(HtmlEscaper.escape(row.description())).append("</td>")
                    .append("<td>").append(row.suppressed() ? "suppressed" : "active").append("</td></tr>");
        }
        return sb.append("</tbody></table></section>").toString();
    }

    private static String cap(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private static String severityBadge(Severity s) {
        return switch (s) {
            case ERROR -> "E";
            case WARN -> "W";
            case INFO -> "I";
        };
    }
}
