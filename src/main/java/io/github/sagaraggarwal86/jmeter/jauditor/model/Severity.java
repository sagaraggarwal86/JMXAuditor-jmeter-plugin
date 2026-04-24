package io.github.sagaraggarwal86.jmeter.jauditor.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Three-level severity. JSON wire value is the lower-cased enum name. */
public enum Severity {
    ERROR, WARN, INFO;

    @JsonValue
    public String asJson() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Human-facing label used by the dialog and the HTML report. */
    public String displayName() {
        return switch (this) {
            case ERROR -> "High";
            case WARN -> "Medium";
            case INFO -> "Low";
        };
    }
}
