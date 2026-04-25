package io.github.sagaraggarwal86.jmeter.jmxauditor.util;

public final class EdtAssertions {
    private EdtAssertions() {
    }

    /**
     * Throws {@link IllegalStateException} if the current thread is not the Swing Event Dispatch Thread.
     * Used by EDT-only mutators to enforce invariant 8 at runtime (not a {@code assert} — production
     * JMeter runs without {@code -ea}, and a silently-disabled check would be worse than no check).
     */
    public static void assertEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("JMXAuditor: expected EDT, was " + Thread.currentThread().getName());
        }
    }
}
