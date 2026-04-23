package io.github.sagaraggarwal86.jmeter.jauditor.ui.action;

import io.github.sagaraggarwal86.jmeter.jauditor.JAuditorPlugin;
import io.github.sagaraggarwal86.jmeter.jauditor.util.JAuditorLog;
import org.apache.jmeter.gui.GuiPackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class ToolbarButtonInstaller {

    private static final JAuditorLog LOG = JAuditorLog.forClass(ToolbarButtonInstaller.class);

    private ToolbarButtonInstaller() {
    }

    public static void installAsync() {
        SwingUtilities.invokeLater(() -> {
            try {
                GuiPackage gp = GuiPackage.getInstance();
                if (gp == null || gp.getMainFrame() == null) return;
                JToolBar tb = findToolBar(gp.getMainFrame());
                if (tb == null) {
                    LOG.warn("toolbar not found — use Tools menu or Ctrl+Shift+A");
                    return;
                }
                tb.add(new JToolBar.Separator());
                JButton btn = new JButton("Audit");
                btn.setToolTipText("Run JAuditor (Ctrl+Shift+A)");
                btn.addActionListener(fireAudit());
                tb.add(btn);
                tb.revalidate();
            } catch (Throwable t) {
                LOG.warn("toolbar button unavailable — use Tools menu or Ctrl+Shift+A", t);
            }
        });
    }

    private static ActionListener fireAudit() {
        return e -> JAuditorMenuCreator.command().doAction(
                new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, JAuditorPlugin.ACTION_ID));
    }

    private static JToolBar findToolBar(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            java.awt.Component child = c.getComponent(i);
            if (child instanceof JToolBar tb) return tb;
            if (child instanceof Container nested) {
                JToolBar found = findToolBar(nested);
                if (found != null) return found;
            }
        }
        return null;
    }
}
