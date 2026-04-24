package io.github.sagaraggarwal86.jmeter.jauditor.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * The six finding categories in PRD §7 order. JSON wire value is the lower-cased
 * enum name (invariant 1); enum order is pinned by
 * {@code RuleRegistryTest.categoryEnumOrderMatchesPrd}.
 */
public enum Category {
    CORRECTNESS,
    SECURITY,
    SCALABILITY,
    REALISM,
    MAINTAINABILITY,
    OBSERVABILITY;

    @JsonValue
    public String asJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
