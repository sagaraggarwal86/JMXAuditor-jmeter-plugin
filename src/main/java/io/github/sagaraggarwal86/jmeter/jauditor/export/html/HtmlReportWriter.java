package io.github.sagaraggarwal86.jmeter.jauditor.export.html;

import io.github.sagaraggarwal86.jmeter.jauditor.export.ReportAggregates;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes the single-file HTML report: token substitution on {@code report-template.html},
 * styled via inlined {@code report-styles.css}, Excel export powered by an inlined
 * xlsx-js-style bundle. Findings that share a rule id within a category panel collapse
 * into a {@code grp-head} row plus hidden {@code grp-member} rows once the count hits
 * {@link #GROUP_THRESHOLD} (currently 3).
 */
public final class HtmlReportWriter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz", Locale.ROOT);
    private static final int GROUP_THRESHOLD = 3;

    private HtmlReportWriter() {
    }

    public static void write(ScanResult r, String pluginVersion, Path out) throws IOException {
        ReportAggregates agg = ReportAggregates.of(r);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("title", "JAuditor Report — " + HtmlEscaper.escape(r.jmxFileName() == null ? "untitled" : r.jmxFileName()));
        tokens.put("jmxFileName", HtmlEscaper.escape(r.jmxFileName() == null ? "(unsaved test plan)" : r.jmxFileName()));
        tokens.put("scanTimestamp", formatTimestamp(r.scanTimestamp()));
        tokens.put("pluginVersion", HtmlEscaper.escape(pluginVersion == null ? "dev" : pluginVersion));
        tokens.put("headerBanners", headerBanners(r));
        tokens.put("navTabs", renderNavTabs(agg));
        tokens.put("panels", renderPanels(agg, r.findings()));

        String html = HtmlTemplate.render(tokens);
        Files.writeString(out, html, StandardCharsets.UTF_8);
    }

    // ─── Header helpers ───────────────────────────────────────

    private static String formatTimestamp(Instant t) {
        return DATE_FMT.format(ZonedDateTime.ofInstant(t, ZoneId.systemDefault()));
    }

    private static String headerBanners(ScanResult r) {
        StringBuilder sb = new StringBuilder();
        if (r.unsavedChanges()) {
            sb.append("<div class=\"banner banner-warn\">⚠ Report generated from unsaved changes. Save the test plan before sharing.</div>");
        }
        if (r.outcome().isTruncated()) {
            sb.append("<div class=\"banner banner-info\">")
                    .append(HtmlEscaper.escape(r.outcome().bannerMessage(r.findings().size())))
                    .append("</div>");
        }
        return sb.toString();
    }

    // ─── Navigation & panels ──────────────────────────────────

    private static String renderNavTabs(ReportAggregates agg) {
        StringBuilder sb = new StringBuilder();
        sb.append(navButton("summary", "Summary", -1, true));
        for (Category c : Category.values()) {
            List<Finding> list = agg.findingsByCategory().get(c);
            if (list.isEmpty()) continue;
            sb.append(navButton(c.name().toLowerCase(Locale.ROOT), cap(c.name()), list.size(), false));
        }
        return sb.toString();
    }

    private static String navButton(String slug, String label, int count, boolean active) {
        String panelId = "panel-" + slug;
        String tabId = "tab-" + slug;
        StringBuilder sb = new StringBuilder();
        sb.append("<button class=\"nav-item").append(active ? " active" : "")
                .append("\" id=\"").append(tabId)
                .append("\" role=\"tab\" aria-selected=\"").append(active).append("\"")
                .append(" aria-controls=\"").append(panelId).append("\"")
                .append(" tabindex=\"").append(active ? "0" : "-1").append("\"")
                .append(" data-panel=\"").append(panelId).append("\">")
                .append(HtmlEscaper.escape(label));
        if (count >= 0) {
            sb.append(" <span class=\"nav-count\">").append(count).append("</span>");
        }
        sb.append("</button>");
        return sb.toString();
    }

    private static String renderPanels(ReportAggregates agg, List<Finding> allFindings) {
        StringBuilder sb = new StringBuilder();

        // Summary panel
        sb.append("<div class=\"panel active\" id=\"panel-summary\" role=\"tabpanel\" aria-labelledby=\"tab-summary\">")
                .append("<h2>Summary</h2>")
                .append(renderCards(agg));
        if (allFindings.isEmpty()) {
            sb.append("<div class=\"empty\">✔ No findings. Script looks clean.</div>");
        }
        sb.append("<details class=\"rule-ref\"><summary>Rule reference (all 25 rules)</summary>")
                .append(renderRulesTable(agg))
                .append("</details></div>");

        int tableIdx = 0;
        for (Category c : Category.values()) {
            List<Finding> list = agg.findingsByCategory().get(c);
            if (list.isEmpty()) continue;
            String slug = c.name().toLowerCase(Locale.ROOT);
            String panelId = "panel-" + slug;
            String tabId = "tab-" + slug;
            String tableId = "t" + (tableIdx++);
            sb.append("<div class=\"panel\" id=\"").append(panelId)
                    .append("\" role=\"tabpanel\" aria-labelledby=\"").append(tabId)
                    .append("\" data-title=\"").append(HtmlEscaper.escape(cap(c.name()))).append("\">")
                    .append("<h2 data-category=\"").append(slug).append("\">")
                    .append(cap(c.name())).append(" <span class=\"muted\">(").append(list.size()).append(")</span></h2>")
                    .append(renderFindingsTable(list, tableId))
                    .append("</div>");
        }
        return sb.toString();
    }

    private static String renderCards(ReportAggregates agg) {
        StringBuilder sb = new StringBuilder("<div class=\"cards\">");
        for (Category c : Category.values()) {
            int count = agg.byCategory().getOrDefault(c, 0);
            String slug = c.name().toLowerCase(Locale.ROOT);
            String name = cap(c.name());
            if (count > 0) {
                sb.append("<button type=\"button\" class=\"card card-link\" data-category=\"").append(slug)
                        .append("\" data-panel=\"panel-").append(slug).append("\"")
                        .append(" aria-label=\"Jump to ").append(HtmlEscaper.escape(name))
                        .append(": ").append(count).append(" finding").append(count == 1 ? "" : "s").append("\">")
                        .append("<div class=\"card-name\">").append(HtmlEscaper.escape(name)).append("</div>")
                        .append("<div class=\"card-count\">").append(count).append("</div>")
                        .append("</button>");
            } else {
                sb.append("<div class=\"card card-empty\" data-category=\"").append(slug).append("\">")
                        .append("<div class=\"card-name\">").append(HtmlEscaper.escape(name)).append("</div>")
                        .append("<div class=\"card-count\">0</div>")
                        .append("</div>");
            }
        }
        return sb.append("</div>").toString();
    }

    // ─── Findings tables ──────────────────────────────────────

    private static String renderFindingsTable(List<Finding> list, String tableId) {
        List<Finding> sorted = new ArrayList<>(list);
        sorted.sort(Comparator
                .comparingInt((Finding f) -> severitySortKey(f.severity()))
                .thenComparing(Finding::ruleId)
                .thenComparingInt(f -> f.nodePath().depth()));

        Map<String, List<Finding>> grouped = new LinkedHashMap<>();
        for (Finding f : sorted) {
            grouped.computeIfAbsent(f.ruleId(), k -> new ArrayList<>()).add(f);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"paginated-tbl\" data-table-id=\"").append(tableId).append("\">");
        sb.append("<div class=\"tbl-controls\">")
                .append("<input type=\"search\" class=\"tbl-search\" data-for=\"").append(tableId)
                .append("\" placeholder=\"Filter findings…\" autocomplete=\"off\" aria-label=\"Filter findings\">")
                .append("<label>Show&nbsp;")
                .append("<select class=\"row-limit\" data-for=\"").append(tableId).append("\">")
                .append("<option value=\"10\" selected>10</option>")
                .append("<option value=\"25\">25</option>")
                .append("<option value=\"50\">50</option>")
                .append("<option value=\"100\">100</option>")
                .append("</select></label>")
                .append("</div>");
        sb.append("<div class=\"pager\" data-for=\"").append(tableId).append("\"></div>");
        sb.append("<div class=\"tbl-empty\" data-for=\"").append(tableId).append("\">No matching findings.</div>");

        sb.append("<table class=\"findings sortable\" data-default-sort=\"0\" data-default-sort-dir=\"asc\"><thead><tr>")
                .append("<th aria-sort=\"ascending\">Severity</th>")
                .append("<th aria-sort=\"none\">Rule ID</th>")
                .append("<th aria-sort=\"none\">Title</th>")
                .append("<th aria-sort=\"none\">Description</th>")
                .append("<th aria-sort=\"none\">Suggestion</th>")
                .append("<th aria-sort=\"none\">Node Path</th>")
                .append("</tr></thead><tbody data-body-id=\"").append(tableId).append("\">");

        for (Map.Entry<String, List<Finding>> e : grouped.entrySet()) {
            List<Finding> group = e.getValue();
            boolean isGroup = group.size() >= GROUP_THRESHOLD;
            for (int i = 0; i < group.size(); i++) {
                Finding f = group.get(i);
                String rowClass;
                if (!isGroup) rowClass = "";
                else if (i == 0) rowClass = " class=\"grp-head\"";
                else rowClass = " class=\"grp-member\" hidden";
                sb.append("<tr").append(rowClass);
                if (isGroup) {
                    sb.append(" data-rule-id=\"").append(HtmlEscaper.escape(f.ruleId())).append("\"");
                }
                sb.append(">");
                sb.append(renderFindingCells(f, isGroup && i == 0, group.size()));
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    private static String renderFindingCells(Finding f, boolean isGroupHead, int groupSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("<td data-sort=\"").append(severitySortKey(f.severity())).append("\">")
                .append("<span class=\"badge ").append(f.severity().asJson()).append("\">")
                .append(severityDisplay(f.severity())).append("</span></td>");
        sb.append("<td><code>").append(HtmlEscaper.escape(f.ruleId())).append("</code></td>");
        sb.append("<td>");
        if (isGroupHead) {
            sb.append("<button type=\"button\" class=\"grp-toggle\" aria-expanded=\"false\" data-rule-id=\"")
                    .append(HtmlEscaper.escape(f.ruleId()))
                    .append("\" aria-label=\"Expand ").append(groupSize).append(" findings for ")
                    .append(HtmlEscaper.escape(f.ruleId())).append("\"></button>");
        }
        sb.append("<strong>").append(HtmlEscaper.escape(f.title())).append("</strong>");
        if (isGroupHead) {
            sb.append("<span class=\"grp-badge\">").append(groupSize).append(" occurrences</span>");
        }
        sb.append("</td>");
        sb.append("<td>").append(HtmlEscaper.escape(f.description() == null ? "" : f.description())).append("</td>");
        sb.append("<td>").append(HtmlEscaper.escape(f.suggestion() == null ? "" : f.suggestion())).append("</td>");
        sb.append("<td><code>").append(HtmlEscaper.escape(f.nodePath().breadcrumb())).append("</code></td>");
        return sb.toString();
    }

    // ─── Rule reference table ─────────────────────────────────

    private static String renderRulesTable(ReportAggregates agg) {
        String tableId = "rules";
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"paginated-tbl\" data-table-id=\"").append(tableId).append("\">");
        sb.append("<div class=\"tbl-controls\">")
                .append("<input type=\"search\" class=\"tbl-search\" data-for=\"").append(tableId)
                .append("\" placeholder=\"Filter rules…\" autocomplete=\"off\" aria-label=\"Filter rules\">")
                .append("<label>Show&nbsp;")
                .append("<select class=\"row-limit\" data-for=\"").append(tableId).append("\">")
                .append("<option value=\"10\" selected>10</option>")
                .append("<option value=\"25\">25</option>")
                .append("<option value=\"50\">50</option>")
                .append("<option value=\"100\">100</option>")
                .append("</select></label>")
                .append("</div>");
        sb.append("<div class=\"pager\" data-for=\"").append(tableId).append("\"></div>");
        sb.append("<div class=\"tbl-empty\" data-for=\"").append(tableId).append("\">No matching rules.</div>");
        sb.append("<table class=\"rules sortable\"><thead><tr>")
                .append("<th aria-sort=\"none\">ID</th>")
                .append("<th aria-sort=\"none\">Category</th>")
                .append("<th aria-sort=\"none\">Severity</th>")
                .append("<th aria-sort=\"none\">Description</th>")
                .append("<th aria-sort=\"none\">Status</th>")
                .append("</tr></thead><tbody data-body-id=\"").append(tableId).append("\">");
        for (ReportAggregates.RuleRow row : agg.rules()) {
            String slug = row.category().name().toLowerCase(Locale.ROOT);
            sb.append("<tr>")
                    .append("<td><code>").append(HtmlEscaper.escape(row.id())).append("</code></td>")
                    .append("<td><span class=\"cat-dot\" data-category=\"").append(slug).append("\" aria-hidden=\"true\"></span>")
                    .append(cap(row.category().name())).append("</td>")
                    .append("<td data-sort=\"").append(severitySortKey(row.severity())).append("\"><span class=\"badge ")
                    .append(row.severity().asJson()).append("\">").append(severityDisplay(row.severity())).append("</span></td>")
                    .append("<td>").append(HtmlEscaper.escape(row.description())).append("</td>")
                    .append("<td>").append(row.suppressed() ? "suppressed" : "active").append("</td>")
                    .append("</tr>");
        }
        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    // ─── Misc helpers ─────────────────────────────────────────

    private static String cap(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String severityDisplay(Severity s) {
        return switch (s) {
            case ERROR -> "High";
            case WARN -> "Medium";
            case INFO -> "Low";
        };
    }

    private static int severitySortKey(Severity s) {
        return switch (s) {
            case ERROR -> 1;
            case WARN -> 2;
            case INFO -> 3;
        };
    }
}
