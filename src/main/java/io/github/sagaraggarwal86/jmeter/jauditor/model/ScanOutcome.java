package io.github.sagaraggarwal86.jmeter.jauditor.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ScanOutcome {
    COMPLETE(null),
    TIMEOUT("Scan exceeded 10s. Partial results shown."),
    NODE_LIMIT("Scanned first 10,000 of %d nodes."),
    FINDING_LIMIT("First 500 of %d findings shown."),
    CANCELLED(null);

    private final String bannerTemplate;

    ScanOutcome(String bannerTemplate) {
        this.bannerTemplate = bannerTemplate;
    }

    public String bannerMessage(int totalCount) {
        if (bannerTemplate == null) return "";
        return bannerTemplate.contains("%d") ? String.format(bannerTemplate, totalCount) : bannerTemplate;
    }

    public boolean isTruncated() {
        return bannerTemplate != null;
    }

    @JsonValue
    public String asJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
