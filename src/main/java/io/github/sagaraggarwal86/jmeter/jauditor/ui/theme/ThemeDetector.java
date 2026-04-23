package io.github.sagaraggarwal86.jmeter.jauditor.ui.theme;

import javax.swing.*;
import java.awt.*;

public final class ThemeDetector {
    private ThemeDetector() {
    }

    public static boolean isDark() {
        Color bg = UIManager.getColor("Panel.background");
        if (bg == null) return false;
        double l = 0.2126 * srgb(bg.getRed()) + 0.7152 * srgb(bg.getGreen()) + 0.0722 * srgb(bg.getBlue());
        return l < 0.5;
    }

    private static double srgb(int v) {
        double c = v / 255.0;
        return (c <= 0.03928) ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
