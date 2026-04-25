package io.github.sagaraggarwal86.jmeter.jmxauditor.ui.dialog;

import javax.swing.*;
import java.awt.*;

public final class HeaderBar extends JPanel {

    private final JButton rescanBtn;
    private final JButton cancelBtn;
    private final JButton exportBtn;

    public HeaderBar(Runnable onRescan, Runnable onCancel, Runnable onExportHtml, Runnable onExportJson) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rescanBtn = new JButton("Rescan");
        cancelBtn = new JButton("Cancel");
        exportBtn = new JButton("Export ▾");
        rescanBtn.addActionListener(e -> onRescan.run());
        cancelBtn.addActionListener(e -> onCancel.run());

        JPopupMenu menu = new JPopupMenu();
        JMenuItem htmlItem = new JMenuItem("Export HTML…");
        htmlItem.addActionListener(e -> onExportHtml.run());
        JMenuItem jsonItem = new JMenuItem("Export JSON…");
        jsonItem.addActionListener(e -> onExportJson.run());
        menu.add(htmlItem);
        menu.add(jsonItem);
        exportBtn.addActionListener(e -> menu.show(exportBtn, 0, exportBtn.getHeight()));

        right.add(rescanBtn);
        right.add(cancelBtn);
        right.add(exportBtn);
        add(right, BorderLayout.EAST);
    }

    public void applyState(DialogState s, boolean hasFindings) {
        switch (s) {
            case IDLE, DONE -> {
                rescanBtn.setVisible(true);
                rescanBtn.setEnabled(true);
                cancelBtn.setVisible(false);
                exportBtn.setEnabled(hasFindings);
            }
            case SCANNING -> {
                rescanBtn.setVisible(false);
                cancelBtn.setVisible(true);
                cancelBtn.setEnabled(true);
                exportBtn.setEnabled(false);
            }
            case CANCELLING -> {
                rescanBtn.setEnabled(false);
                cancelBtn.setEnabled(false);
                exportBtn.setEnabled(false);
            }
        }
    }
}
