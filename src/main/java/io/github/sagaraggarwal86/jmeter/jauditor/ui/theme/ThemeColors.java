package io.github.sagaraggarwal86.jmeter.jauditor.ui.theme;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public final class ThemeColors {

    private static final Map<Category, Color> LIGHT = new EnumMap<>(Category.class);
    private static final Map<Category, Color> DARK = new EnumMap<>(Category.class);

    static {
        LIGHT.put(Category.CORRECTNESS, new Color(0xC62828));
        LIGHT.put(Category.SECURITY, new Color(0xE57373));
        LIGHT.put(Category.SCALABILITY, new Color(0xF9A825));
        LIGHT.put(Category.REALISM, new Color(0x00897B));
        LIGHT.put(Category.MAINTAINABILITY, new Color(0x7B1FA2));
        LIGHT.put(Category.OBSERVABILITY, new Color(0x1565C0));

        DARK.put(Category.CORRECTNESS, new Color(0xEF5350));
        DARK.put(Category.SECURITY, new Color(0xFF8A80));
        DARK.put(Category.SCALABILITY, new Color(0xFFD54F));
        DARK.put(Category.REALISM, new Color(0x4DB6AC));
        DARK.put(Category.MAINTAINABILITY, new Color(0xBA68C8));
        DARK.put(Category.OBSERVABILITY, new Color(0x64B5F6));
    }

    private ThemeColors() {
    }

    public static Color forCategory(Category c) {
        return ThemeDetector.isDark() ? DARK.get(c) : LIGHT.get(c);
    }

    public static String cssHexLight(Category c) {
        return String.format("#%06X", LIGHT.get(c).getRGB() & 0xFFFFFF);
    }
}
