package io.github.sagaraggarwal86.jmeter.jmxauditor.ui.dialog;

import javax.swing.*;
import java.awt.*;

public final class FooterBar extends JPanel {

    private final JLabel left;
    private final JLabel right;
    private final JLabel hiddenIndicator;
    private final JButton showAllBtn;

    public FooterBar(String version, Runnable onShowAll) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        left = new JLabel("—");
        add(left, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        hiddenIndicator = new JLabel("");
        showAllBtn = new JButton("Show all");
        showAllBtn.setVisible(false);
        showAllBtn.addActionListener(e -> onShowAll.run());
        right = new JLabel("v" + version);
        rightPanel.add(hiddenIndicator);
        rightPanel.add(showAllBtn);
        rightPanel.add(right);
        add(rightPanel, BorderLayout.EAST);
    }

    public void setMetadata(long durationMs, int nodes, int rules) {
        String dur = (durationMs < 1000) ? durationMs + " ms" : (durationMs / 1000L) + " s";
        left.setText("<html>Last scan: " + dur + " · " + nodes + " nodes analyzed · "
                + rules + " rules · <i>Tip: Export HTML for detailed findings</i></html>");
    }

    public void setHiddenCount(int n) {
        if (n == 0) {
            hiddenIndicator.setText("");
            showAllBtn.setVisible(false);
        } else {
            hiddenIndicator.setText(n + " rule(s) suppressed for this session.");
            showAllBtn.setVisible(true);
        }
    }
}
