package io.github.sagaraggarwal86.jmeter.jauditor.ui.action;

import io.github.sagaraggarwal86.jmeter.jauditor.JAuditorPlugin;
import io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog.AuditDialog;
import io.github.sagaraggarwal86.jmeter.jauditor.util.JAuditorLog;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.action.AbstractAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Set;

public final class AuditCommand extends AbstractAction {

    private static final JAuditorLog LOG = JAuditorLog.forClass(AuditCommand.class);
    private static final Set<String> ACTIONS = Set.of(JAuditorPlugin.ACTION_ID);
    private static AuditDialog currentDialog;

    @Override
    public void doAction(ActionEvent e) {
        try {
            GuiPackage gp = GuiPackage.getInstance();
            if (gp == null) return;
            if (gp.getTreeModel() == null || gp.getTreeModel().getTestPlan() == null
                    || gp.getTreeModel().getTestPlan().size() == 0) {
                JOptionPane.showMessageDialog(gp.getMainFrame(),
                        "Open a test plan first to audit it.",
                        "JAuditor", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (currentDialog != null && currentDialog.isDisplayable()) {
                currentDialog.toFront();
                currentDialog.startScan();
                return;
            }
            currentDialog = new AuditDialog(gp.getMainFrame());
            currentDialog.setVisible(true);
            currentDialog.startScan();
        } catch (Exception ex) {
            LOG.error("audit command failed", ex);
            GuiPackage gp = GuiPackage.getInstance();
            if (gp != null) {
                JOptionPane.showMessageDialog(gp.getMainFrame(),
                        "An unexpected error occurred. Check jmeter.log.",
                        "JAuditor", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public Set<String> getActionNames() {
        return ACTIONS;
    }
}
