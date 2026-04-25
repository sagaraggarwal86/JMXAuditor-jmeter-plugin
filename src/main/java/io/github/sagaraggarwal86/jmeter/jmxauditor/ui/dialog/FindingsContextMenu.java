package io.github.sagaraggarwal86.jmeter.jmxauditor.ui.dialog;

import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.function.Consumer;

public final class FindingsContextMenu {

    private FindingsContextMenu() {
    }

    public static JPopupMenu build(Finding f, Consumer<Finding> onNavigate, Consumer<String> onHideRule) {
        JPopupMenu m = new JPopupMenu();
        JMenuItem go = new JMenuItem("Go to element");
        go.addActionListener(e -> onNavigate.accept(f));
        JMenuItem copyAll = new JMenuItem("Copy finding details");
        copyAll.addActionListener(e -> copy(f.title() + "\n" + f.nodePath().breadcrumb() + "\n" + f.suggestion()));
        JMenuItem copyPath = new JMenuItem("Copy node path");
        copyPath.addActionListener(e -> copy(f.nodePath().breadcrumb()));
        JMenuItem hide = new JMenuItem("Hide this rule for this session");
        hide.addActionListener(e -> onHideRule.accept(f.ruleId()));
        m.add(go);
        m.add(copyAll);
        m.add(copyPath);
        m.addSeparator();
        m.add(hide);
        return m;
    }

    private static void copy(String s) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
    }
}
