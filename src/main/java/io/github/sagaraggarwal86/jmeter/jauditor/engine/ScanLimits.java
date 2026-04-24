package io.github.sagaraggarwal86.jmeter.jauditor.engine;

/**
 * Single source of truth for the three hard scan caps (invariant 12).
 * {@link TreeWalker#walk} checks all three at every node boundary; exceeding any of
 * them results in a {@code ScanOutcome} that triggers the truncation banner.
 */
public final class ScanLimits {
    public static final int MAX_NODES = 10_000;
    public static final int MAX_FINDINGS = 500;
    public static final long MAX_SCAN_MILLIS = 10_000L;

    private ScanLimits() {
    }
}
