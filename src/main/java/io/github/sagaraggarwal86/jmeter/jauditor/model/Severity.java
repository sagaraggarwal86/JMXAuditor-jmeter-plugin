package io.github.sagaraggarwal86.jmeter.jauditor.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Three-level severity used by all 25 rules. JSON wire value (invariant 1) is the
 * lower-cased enum name; the HTML report renders {@code ERROR / WARN / INFO} as
 * {@code High / Medium / Low}.
 */
public enum Severity {
    ERROR, WARN, INFO;

    @JsonValue
    public String asJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
