package io.github.sagaraggarwal86.jmeter.jauditor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J wrapper prefixing every message with {@code "JAuditor: "} so plugin-originated
 * lines are greppable in {@code jmeter.log}. {@link #redact(String)} collapses any
 * credential value to {@code "****"} — the three Security rules that carry credential
 * text into a finding description MUST route through it (invariant 9).
 */
public final class JAuditorLog {

    private static final String PREFIX = "JAuditor: ";
    private static final String REDACTED = "****";

    private final Logger delegate;

    private JAuditorLog(Logger delegate) {
        this.delegate = delegate;
    }

    public static JAuditorLog forClass(Class<?> c) {
        return new JAuditorLog(LoggerFactory.getLogger(c));
    }

    /**
     * Returns the redacted sentinel {@code "****"} regardless of input. The parameter
     * exists so call sites read naturally ({@code redact(password)}) and to document,
     * statically, that a credential was handled; the actual value is intentionally
     * discarded to keep it out of every downstream surface (log, finding text, report).
     */
    public static String redact(String value) {
        return REDACTED;
    }

    public void info(String msg) {
        delegate.info(PREFIX + msg);
    }

    public void info(String fmt, Object... args) {
        delegate.info(PREFIX + fmt, args);
    }

    public void debug(String msg) {
        delegate.debug(PREFIX + msg);
    }

    public void debug(String fmt, Object... args) {
        delegate.debug(PREFIX + fmt, args);
    }

    public void warn(String msg) {
        delegate.warn(PREFIX + msg);
    }

    public void warn(String msg, Throwable t) {
        delegate.warn(PREFIX + msg, t);
    }

    public void warn(String fmt, Object... args) {
        delegate.warn(PREFIX + fmt, args);
    }

    public void error(String msg) {
        delegate.error(PREFIX + msg);
    }

    public void error(String msg, Throwable t) {
        delegate.error(PREFIX + msg, t);
    }

    public void logScanSummary(int findings, int nodes, long durationMs, String outcome) {
        delegate.info(PREFIX + "scan complete: {} findings, {} nodes, {} ms, outcome={}",
                findings, nodes, durationMs, outcome);
    }
}
