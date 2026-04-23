package io.github.sagaraggarwal86.jmeter.jauditor.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

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
