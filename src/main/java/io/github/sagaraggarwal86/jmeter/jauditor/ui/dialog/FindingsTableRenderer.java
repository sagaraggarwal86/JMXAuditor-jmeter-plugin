package io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.ui.theme.ThemeColors;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public final class FindingsTableRenderer implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Finding f = (Finding) value;
        JPanel root = new JPanel(new BorderLayout(0, 0));
        Color stripe = ThemeColors.forCategory(f.category());
        root.setBackground(isSelected ? UIManager.getColor("Table.selectionBackground") : UIManager.getColor("Table.background"));
        root.setBorder(BorderFactory.createMatteBorder(0, 3, 1, 0, stripe));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        text.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel title = new JLabel("[" + f.category().name() + "] " + f.title());
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setForeground(isSelected ? UIManager.getColor("Table.selectionForeground") : UIManager.getColor("Label.foreground"));
        text.add(title);

        JLabel path = new JLabel(f.nodePath().breadcrumb());
        path.setFont(new Font(Font.MONOSPACED, Font.PLAIN, path.getFont().getSize() - 1));
        text.add(path);

        if (f.suggestion() != null && !f.suggestion().isBlank()) {
            JLabel suggestion = new JLabel(f.suggestion());
            suggestion.setFont(suggestion.getFont().deriveFont(Font.ITALIC));
            text.add(suggestion);
        }

        root.add(text, BorderLayout.CENTER);
        root.setPreferredSize(new Dimension(400, 64));
        table.setRowHeight(row, 64);
        return root;
    }
}
