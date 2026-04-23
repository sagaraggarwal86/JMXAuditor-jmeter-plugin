package io.github.sagaraggarwal86.jmeter.jauditor.util;

import javax.swing.*;

public final class EdtAssertions {
    private EdtAssertions() {
    }

    public static void assertEdt() {
        assert SwingUtilities.isEventDispatchThread() : "JAuditor: expected EDT";
    }
}
