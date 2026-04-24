package io.github.sagaraggarwal86.jmeter.jauditor.engine;

/** Single source for the three hard scan caps; {@link TreeWalker} checks all three per node boundary. */
public final class ScanLimits {
    public static final int MAX_NODES = 10_000;
    public static final int MAX_FINDINGS = 500;
    public static final long MAX_SCAN_MILLIS = 10_000L;

    private ScanLimits() {
    }
}
