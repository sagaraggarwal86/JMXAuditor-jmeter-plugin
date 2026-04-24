package io.github.sagaraggarwal86.jmeter.jauditor.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Terminal state of a scan. {@link #COMPLETE} and {@link #CANCELLED} carry no banner;
 * the three truncation outcomes ({@link #TIMEOUT}, {@link #NODE_LIMIT},
 * {@link #FINDING_LIMIT}) produce user-visible text via {@link #bannerMessage(int)}.
 * Banner strings live here so the dialog and the HTML report render identical wording.
 */
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
