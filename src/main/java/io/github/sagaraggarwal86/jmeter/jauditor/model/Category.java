package io.github.sagaraggarwal86.jmeter.jauditor.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * The six finding categories in PRD §7 order. JSON wire value is the lower-cased enum name.
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

    /**
     * Title-case label used by dialog + HTML report (e.g. {@code "Correctness"}).
     */
    public String displayName() {
        String s = name();
        return s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
