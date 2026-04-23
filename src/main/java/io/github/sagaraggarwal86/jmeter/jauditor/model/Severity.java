package io.github.sagaraggarwal86.jmeter.jauditor.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Severity {
    ERROR, WARN, INFO;

    @JsonValue
    public String asJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
