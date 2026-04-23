package io.github.sagaraggarwal86.jmeter.jauditor.export;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.Rule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.RuleRegistry;

import java.util.*;

/**
 * Single pass over findings + rule registry — shared by HTML and JSON writers.
 */
public record ReportAggregates(
        EnumMap<Category, Integer> byCategory,
        EnumMap<Severity, Integer> bySeverity,
        Map<Category, List<Finding>> findingsByCategory,
        List<RuleRow> rules
) {

    public static ReportAggregates of(ScanResult r) {
        EnumMap<Category, Integer> cat = new EnumMap<>(Category.class);
        EnumMap<Severity, Integer> sev = new EnumMap<>(Severity.class);
        for (Category c : Category.values()) cat.put(c, 0);
        for (Severity s : Severity.values()) sev.put(s, 0);

        Map<Category, List<Finding>> byCat = new LinkedHashMap<>();
        for (Category c : Category.values()) byCat.put(c, new ArrayList<>());

        for (Finding f : r.findings()) {
            cat.merge(f.category(), 1, Integer::sum);
            sev.merge(f.severity(), 1, Integer::sum);
            byCat.get(f.category()).add(f);
        }

        List<RuleRow> rules = new ArrayList<>();
        for (Rule rule : RuleRegistry.all()) {
            rules.add(new RuleRow(rule.id(), rule.category(), rule.severity(),
                    rule.description(), r.suppressedRuleIds().contains(rule.id())));
        }

        return new ReportAggregates(cat, sev, byCat, rules);
    }

    public record RuleRow(String id, Category category, Severity severity, String description, boolean suppressed) {
    }
}
