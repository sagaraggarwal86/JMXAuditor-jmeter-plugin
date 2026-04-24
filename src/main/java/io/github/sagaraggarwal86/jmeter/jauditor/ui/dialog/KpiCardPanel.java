package io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.ui.theme.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class KpiCardPanel extends JPanel {

    private final Map<Category, JToggleButton> buttons = new EnumMap<>(Category.class);
    private final Map<Category, Integer> counts = new EnumMap<>(Category.class);
    private final EnumSet<Category> selected = EnumSet.allOf(Category.class);
    private Consumer<Set<Category>> listener = s -> {
    };

    public KpiCardPanel() {
        setLayout(new WrapLayout(FlowLayout.LEFT, 4, 4));
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        for (Category c : Category.values()) {
            counts.put(c, 0);
            add(buildButton(c));
        }
    }

    private static String labelFor(Category c, int count) {
        return c.displayName() + " (" + count + ")";
    }

    private static Font baseLabelFont(JComponent fallback) {
        Font f = UIManager.getFont("Label.font");
        return (f != null) ? f : fallback.getFont();
    }

    private static void updateAccessibleDescription(JToggleButton b, boolean sel, int count) {
        b.getAccessibleContext().setAccessibleDescription(
                count + " findings. " + (sel ? "Shown. Press Space to hide." : "Hidden. Press Space to show."));
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
            JToggleButton b = buttons.get(c);
            if (!b.isSelected()) b.setSelected(true);
            updateAccessibleDescription(b, true, counts.get(c));
        }
        listener.accept(EnumSet.copyOf(selected));
    }

    public void toggleCategory(Category c) {
        JToggleButton b = buttons.get(c);
        if (b != null) b.doClick();
    }

    public void update(java.util.List<Finding> findings) {
        EnumMap<Category, Integer> byCat = new EnumMap<>(Category.class);
        for (Category c : Category.values()) byCat.put(c, 0);
        for (Finding f : findings) byCat.merge(f.category(), 1, Integer::sum);
        byCat.forEach((c, n) -> {
            counts.put(c, n);
            JToggleButton b = buttons.get(c);
            b.setText(labelFor(c, n));
            updateAccessibleDescription(b, b.isSelected(), n);
        });
    }

    private JToggleButton buildButton(Category c) {
        JToggleButton btn = new JToggleButton(labelFor(c, 0));
        btn.setSelected(true);
        btn.setIcon(new ColorSquareIcon(ThemeColors.forCategory(c)));
        btn.setIconTextGap(6);
        btn.setFont(baseLabelFont(btn));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.getAccessibleContext().setAccessibleName(c.displayName() + " filter");
        updateAccessibleDescription(btn, true, 0);

        btn.addActionListener(e -> {
            if (btn.isSelected()) selected.add(c);
            else selected.remove(c);
            updateAccessibleDescription(btn, btn.isSelected(), counts.get(c));
            listener.accept(EnumSet.copyOf(selected));
        });

        buttons.put(c, btn);
        return btn;
    }

    private static final class ColorSquareIcon implements Icon {
        private static final int SIZE = 10;
        private final Color color;

        ColorSquareIcon(Color color) {
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color old = g.getColor();
            g.setColor(color);
            g.fillRect(x, y, SIZE, SIZE);
            g.setColor(color.darker());
            g.drawRect(x, y, SIZE - 1, SIZE - 1);
            g.setColor(old);
        }
    }
}
