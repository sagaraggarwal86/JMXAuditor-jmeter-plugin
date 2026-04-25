package io.github.sagaraggarwal86.jmeter.jmxauditor.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FindingExtraTest {

    @Test
    void ruleFailureProducesInfoFinding() {
        NodePath p = new NodePath(List.of("Test Plan", "Boom"));
        RuntimeException cause = new RuntimeException("kaboom");
        Finding f = Finding.ruleFailure("RULE_X", Category.SECURITY, p, cause);
        assertThat(f.severity()).isEqualTo(Severity.INFO);
        assertThat(f.ruleId()).isEqualTo("RULE_X");
        assertThat(f.category()).isEqualTo(Category.SECURITY);
        assertThat(f.title()).isEqualTo("Rule execution failed");
        assertThat(f.description()).contains("RuntimeException");
        assertThat(f.suggestion()).contains("jmeter.log");
        assertThat(f.nodePath()).isEqualTo(p);
        assertThat(f.treeDepth()).isEqualTo(2);
    }

    @Test
    void rejectsNullRequiredFields() {
        NodePath p = new NodePath(List.of("Test Plan"));
        assertThatThrownBy(() -> new Finding(null, Category.CORRECTNESS, Severity.ERROR, "t", "d", "s", p, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Finding("X", null, Severity.ERROR, "t", "d", "s", p, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Finding("X", Category.CORRECTNESS, null, "t", "d", "s", p, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Finding("X", Category.CORRECTNESS, Severity.ERROR, "t", "d", "s", null, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void severityDisplayNames() {
        assertThat(Severity.ERROR.displayName()).isEqualTo("High");
        assertThat(Severity.WARN.displayName()).isEqualTo("Medium");
        assertThat(Severity.INFO.displayName()).isEqualTo("Low");
    }

    @Test
    void categoryDisplayNamesAreTitleCase() {
        assertThat(Category.CORRECTNESS.displayName()).isEqualTo("Correctness");
        assertThat(Category.SECURITY.displayName()).isEqualTo("Security");
        assertThat(Category.MAINTAINABILITY.displayName()).isEqualTo("Maintainability");
    }

    @Test
    void scanOutcomeAsJsonIsLowercase() {
        assertThat(ScanOutcome.COMPLETE.asJson()).isEqualTo("complete");
        assertThat(ScanOutcome.NODE_LIMIT.asJson()).isEqualTo("node_limit");
        assertThat(ScanOutcome.CANCELLED.asJson()).isEqualTo("cancelled");
        // Non-truncated outcomes return empty banner messages.
        assertThat(ScanOutcome.COMPLETE.bannerMessage(0)).isEmpty();
        assertThat(ScanOutcome.CANCELLED.bannerMessage(7)).isEmpty();
        assertThat(ScanOutcome.CANCELLED.isTruncated()).isFalse();
    }
}
