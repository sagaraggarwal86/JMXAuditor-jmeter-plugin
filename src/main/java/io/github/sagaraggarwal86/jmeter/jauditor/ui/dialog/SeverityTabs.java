package io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class SeverityTabs extends JPanel {

    private final JToggleButton all;
    private final JToggleButton error;
    private final JToggleButton warn;
    private final JToggleButton info;

    public SeverityTabs() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        all = new JToggleButton("All (0)", true);
        error = new JToggleButton("Error (0)");
        warn = new JToggleButton("Warn (0)");
        info = new JToggleButton("Info (0)");
        ButtonGroup g = new ButtonGroup();
        g.add(all);
        g.add(error);
        g.add(warn);
        g.add(info);
        add(all);
        add(error);
        add(warn);
        add(info);
    }

    public void setFilterListener(Consumer<FindingsTableModel.Filter> onChange) {
        all.addActionListener(e -> onChange.accept(FindingsTableModel.Filter.ALL));
        error.addActionListener(e -> onChange.accept(FindingsTableModel.Filter.ERROR));
        warn.addActionListener(e -> onChange.accept(FindingsTableModel.Filter.WARN));
        info.addActionListener(e -> onChange.accept(FindingsTableModel.Filter.INFO));
    }

    public void resetToAll() {
        all.setSelected(true);
    }

    public void selectFilter(FindingsTableModel.Filter f) {
        JToggleButton target = switch (f) {
            case ALL -> all;
            case ERROR -> error;
            case WARN -> warn;
            case INFO -> info;
        };
        target.doClick();
    }

    public void updateCounts(FindingsTableModel model) {
        all.setText("All (" + model.countAll() + ")");
        error.setText("Error (" + model.countSeverity(Severity.ERROR) + ")");
        warn.setText("Warn (" + model.countSeverity(Severity.WARN) + ")");
        info.setText("Info (" + model.countSeverity(Severity.INFO) + ")");
    }
}
