package io.github.sagaraggarwal86.jmeter.jauditor.util;

import org.apache.jmeter.gui.GuiPackage;

/**
 * Guards UI code paths against being entered from CLI mode. {@link #isGuiMode()}
 * returns {@code false} when {@code GuiPackage.getInstance() == null} — the plugin
 * is inert outside GUI mode per the zero-runtime-impact contract (invariant 3).
 */
public final class GuiGuard {
    private GuiGuard() {
    }

    public static boolean isGuiMode() {
        return GuiPackage.getInstance() != null;
    }

    public static GuiPackage require() {
        GuiPackage gp = GuiPackage.getInstance();
        if (gp == null) {
            throw new IllegalStateException("JAuditor: GuiPackage unavailable (CLI mode?)");
        }
        return gp;
    }
}
