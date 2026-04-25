package io.github.sagaraggarwal86.jmeter.jmxauditor.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuiGuardTest {

    // GuiPackage is unavailable in unit tests (no JMeter GUI bootstrap),
    // so these tests exercise the CLI/no-GUI branch of GuiGuard.

    @Test
    void isGuiModeFalseWhenGuiPackageMissing() {
        assertThat(GuiGuard.isGuiMode()).isFalse();
    }

    @Test
    void requireThrowsWhenGuiPackageMissing() {
        assertThatThrownBy(GuiGuard::require)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GuiPackage unavailable");
    }
}
