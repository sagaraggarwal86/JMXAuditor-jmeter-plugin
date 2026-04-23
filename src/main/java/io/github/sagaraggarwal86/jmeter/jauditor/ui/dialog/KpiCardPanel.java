package io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.ui.theme.ThemeColors;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class KpiCardPanel extends JPanel {

    private final Map<Category, JLabel> counts = new EnumMap<>(Category.class);
    private final Map<Category, JLabel> names = new EnumMap<>(Category.class);
    private final Map<Category, JToggleButton> cards = new EnumMap<>(Category.class);
    private final EnumSet<Category> selected = EnumSet.allOf(Category.class);
    private Consumer<Set<Category>> listener = s -> {
    };

    public KpiCardPanel() {
        setLayout(new GridLayout(1, 6, 8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (Category c : Category.values()) add(buildCard(c));
    }

    private static String capitalize(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private static Font baseLabelFont(JComponent fallback) {
        Font f = UIManager.getFont("Label.font");
        return (f != null) ? f : fallback.getFont();
    }

    public void setSelectionListener(Consumer<Set<Category>> l) {
        this.listener = (l != null) ? l : s -> {
        };
    }

    public Set<Category> selectedCategories() {
        return EnumSet.copyOf(selected);
    }

    public void resetSelection() {
        selected.clear();
        selected.addAll(EnumSet.allOf(Category.class));
        for (Category c : Category.values()) {
            JToggleButton b = cards.get(c);
            if (!b.isSelected()) b.setSelected(true);
            applySelectionVisual(c);
            updateAccessibleDescription(c);
        }
        listener.accept(EnumSet.copyOf(selected));
    }

    public void toggleCategory(Category c) {
        JToggleButton b = cards.get(c);
        if (b != null) b.doClick();
    }

    public void update(java.util.List<Finding> findings) {
        EnumMap<Category, Integer> byCat = new EnumMap<>(Category.class);
        for (Category c : Category.values()) byCat.put(c, 0);
        for (Finding f : findings) byCat.merge(f.category(), 1, Integer::sum);
        byCat.forEach((c, n) -> {
            counts.get(c).setText(Integer.toString(n));
            updateAccessibleDescription(c);
        });
    }

    private JToggleButton buildCard(Category c) {
        JToggleButton btn = new JToggleButton();
        btn.setSelected(true);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setRolloverEnabled(false);
        btn.setOpaque(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setLayout(new BoxLayout(btn, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(capitalize(c.name()));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        Font base = baseLabelFont(name);
        name.setFont(base.deriveFont(Font.BOLD));

        JLabel count = new JLabel("0");
        count.setAlignmentX(Component.CENTER_ALIGNMENT);
        count.setFont(base.deriveFont(Font.BOLD, base.getSize2D() * 1.5f));

        btn.add(Box.createVerticalGlue());
        btn.add(name);
        btn.add(count);
        btn.add(Box.createVerticalGlue());

        counts.put(c, count);
        names.put(c, name);
        cards.put(c, btn);

        AccessibleContext ac = btn.getAccessibleContext();
        ac.setAccessibleName(capitalize(c.name()) + " filter");

        btn.addActionListener(e -> {
            if (btn.isSelected()) selected.add(c);
            else selected.remove(c);
            applySelectionVisual(c);
            updateAccessibleDescription(c);
            listener.accept(EnumSet.copyOf(selected));
        });

        btn.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                applySelectionVisual(c);
            }

            @Override
            public void focusLost(FocusEvent e) {
                applySelectionVisual(c);
            }
        });

        applySelectionVisual(c);
        updateAccessibleDescription(c);
        return btn;
    }

    private void applySelectionVisual(Category c) {
        JToggleButton card = cards.get(c);
        JLabel name = names.get(c);
        JLabel count = counts.get(c);
        boolean sel = selected.contains(c);
        boolean focused = card.isFocusOwner();
        Color stripe = ThemeColors.forCategory(c);
        Color dim = UIManager.getColor("Label.disabledForeground");
        if (dim == null) dim = Color.GRAY;
        Color focusColor = UIManager.getColor("Component.focusColor");
        if (focusColor == null) focusColor = UIManager.getColor("Focus.color");
        if (focusColor == null) focusColor = stripe;

        Color edge = sel ? stripe : dim;
        Border outer = BorderFactory.createLineBorder(edge, 2, true);
        Border padding = focused
                ? BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(focusColor, 1f, 2f, 2f, true),
                BorderFactory.createEmptyBorder(7, 9, 7, 9))
                : BorderFactory.createEmptyBorder(8, 10, 8, 10);
        card.setBorder(BorderFactory.createCompoundBorder(outer, padding));

        Color fg = sel ? UIManager.getColor("Label.foreground") : dim;
        if (fg == null) fg = sel ? Color.BLACK : Color.GRAY;
        name.setForeground(fg);
        count.setForeground(fg);
    }

    private void updateAccessibleDescription(Category c) {
        JToggleButton card = cards.get(c);
        boolean sel = selected.contains(c);
        String n = counts.get(c).getText();
        card.getAccessibleContext().setAccessibleDescription(
                n + " findings. " + (sel ? "Shown. Press Space to hide." : "Hidden. Press Space to show."));
    }
}
