package io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog;

import javax.swing.*;
import java.awt.*;

public final class HeaderBar extends JPanel {

    private final JLabel title;
    private final JButton rescanBtn;
    private final JButton cancelBtn;
    private final JButton exportHtmlBtn;
    private final JButton exportDropdownBtn;
    private final JMenuItem exportJsonItem;

    public HeaderBar(Runnable onRescan, Runnable onCancel, Runnable onExportHtml, Runnable onExportJson) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        title = new JLabel("JAuditor");
        add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rescanBtn = new JButton("Rescan");
        cancelBtn = new JButton("Cancel");
        exportHtmlBtn = new JButton("Export HTML");
        exportDropdownBtn = new JButton("▾");
        rescanBtn.addActionListener(e -> onRescan.run());
        cancelBtn.addActionListener(e -> onCancel.run());
        exportHtmlBtn.addActionListener(e -> onExportHtml.run());

        JPopupMenu menu = new JPopupMenu();
        exportJsonItem = new JMenuItem("Export JSON…");
        exportJsonItem.addActionListener(e -> onExportJson.run());
        menu.add(exportJsonItem);
        exportDropdownBtn.addActionListener(e -> menu.show(exportDropdownBtn, 0, exportDropdownBtn.getHeight()));

        right.add(rescanBtn);
        right.add(cancelBtn);
        right.add(exportHtmlBtn);
        right.add(exportDropdownBtn);
        add(right, BorderLayout.EAST);
    }

    public void setTitleText(String jmxName, boolean dirty, boolean untitled) {
        if (untitled) title.setText("JAuditor — untitled");
        else title.setText("JAuditor — " + jmxName + (dirty ? " •" : ""));
    }

    public void applyState(DialogState s, boolean hasFindings) {
        switch (s) {
            case IDLE, DONE -> {
                rescanBtn.setVisible(true);
                rescanBtn.setEnabled(true);
                cancelBtn.setVisible(false);
                exportHtmlBtn.setEnabled(hasFindings);
                exportDropdownBtn.setEnabled(hasFindings);
            }
            case SCANNING -> {
                rescanBtn.setVisible(false);
                cancelBtn.setVisible(true);
                cancelBtn.setEnabled(true);
                exportHtmlBtn.setEnabled(false);
                exportDropdownBtn.setEnabled(false);
            }
            case CANCELLING -> {
                rescanBtn.setEnabled(false);
                cancelBtn.setEnabled(false);
                exportHtmlBtn.setEnabled(false);
                exportDropdownBtn.setEnabled(false);
            }
        }
    }
}
