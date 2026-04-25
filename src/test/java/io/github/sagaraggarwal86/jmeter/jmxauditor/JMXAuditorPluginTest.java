package io.github.sagaraggarwal86.jmeter.jmxauditor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JMXAuditorPluginTest {

    @Test
    void identityConstants() {
        assertThat(JMXAuditorPlugin.NAME).isEqualTo("JMXAuditor");
        assertThat(JMXAuditorPlugin.ACTION_ID).isEqualTo("jmxauditor.audit");
    }

    @Test
    void versionResolvesNonEmptyAndNotPlaceholder() {
        // Maven filters version.properties at build time, so during a real build
        // the value is a semver string. In an IDE without a fresh `mvn compile`,
        // the resource may carry the literal "${project.version}" — version()
        // detects that and falls through to the manifest / "dev" fallback.
        String v = JMXAuditorPlugin.version();
        assertThat(v).isNotBlank();
        assertThat(v).doesNotContain("${");
    }

    @Test
    void buildTimestampStrippedOfPlaceholder() {
        String t = JMXAuditorPlugin.buildTimestamp();
        // Either a real timestamp or empty string — never the unfiltered token.
        assertThat(t).doesNotContain("${");
    }
}
