package io.github.sagaraggarwal86.jmeter.jmxauditor.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EdtAssertionsTest {

    @Test
    void throwsWhenNotOnEdt() {
        // The current test thread is not the EDT.
        assertThatThrownBy(EdtAssertions::assertEdt)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected EDT");
    }

    @Test
    void passesWhenOnEdt() throws Exception {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                EdtAssertions.assertEdt();
            } catch (Throwable t) {
                caught.set(t);
            }
        });
        assertThat(caught.get()).isNull();
    }
}
