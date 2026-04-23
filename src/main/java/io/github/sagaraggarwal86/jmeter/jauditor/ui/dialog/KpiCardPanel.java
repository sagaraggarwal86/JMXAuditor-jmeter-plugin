package io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.ui.theme.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public final class KpiCardPanel extends JPanel {

    private final Map<Category, JLabel> counts = new EnumMap<>(Category.class);

    public KpiCardPanel() {
        setLayout(new GridLayout(1, 6, 8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (Category c : Category.values()) add(buildCard(c));
    }

    private static String capitalize(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private JPanel buildCard(Category c) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeColors.forCategory(c), 2, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        JLabel name = new JLabel(capitalize(c.name()));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        JLabel count = new JLabel("0");
        count.setAlignmentX(Component.CENTER_ALIGNMENT);
        count.setFont(count.getFont().deriveFont(Font.BOLD, 20f));
        counts.put(c, count);
        card.add(name);
        card.add(count);
        return card;
    }

    public void update(java.util.List<Finding> findings) {
        EnumMap<Category, Integer> byCat = new EnumMap<>(Category.class);
        for (Category c : Category.values()) byCat.put(c, 0);
        for (Finding f : findings) byCat.merge(f.category(), 1, Integer::sum);
        byCat.forEach((c, n) -> counts.get(c).setText(Integer.toString(n)));
    }
}
