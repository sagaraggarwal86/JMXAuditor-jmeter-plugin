package io.github.sagaraggarwal86.jmeter.jauditor.util;

import org.apache.jmeter.gui.GuiPackage;

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
