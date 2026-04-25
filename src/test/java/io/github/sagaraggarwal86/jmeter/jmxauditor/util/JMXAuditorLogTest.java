package io.github.sagaraggarwal86.jmeter.jmxauditor.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JMXAuditorLogTest {

    @Test
    void redactNullReturnsSentinel() {
        assertThat(JMXAuditorLog.redact(null)).isEqualTo("****");
    }

    @Test
    void redactEmptyReturnsSentinel() {
        assertThat(JMXAuditorLog.redact("")).isEqualTo("****");
    }

    @Test
    void redactNonEmptyReturnsSentinel() {
        // The whole point: caller's value never reaches the sink, regardless of length / content.
        assertThat(JMXAuditorLog.redact("super-secret-token-abc123!@#")).isEqualTo("****");
        assertThat(JMXAuditorLog.redact("a")).isEqualTo("****");
    }

    @Test
    void forClassReturnsUsableLog() {
        JMXAuditorLog log = JMXAuditorLog.forClass(JMXAuditorLogTest.class);
        // Each level is a thin SLF4J pass-through with the "JMXAuditor: " prefix.
        // Calling them must not throw — that's all we can verify without an SLF4J appender capture.
        log.info("info no args");
        log.info("info {} args", "with");
        log.debug("debug no args");
        log.debug("debug {} args", "with");
        log.warn("warn no args");
        log.warn("warn with throwable", new RuntimeException("x"));
        log.warn("warn {} args", "with");
        log.error("error no args");
        log.error("error with throwable", new RuntimeException("x"));
        log.logScanSummary(3, 42, 17L, "COMPLETE");
    }
}
