package io.github.sagaraggarwal86.jmeter.jmxauditor.ui.navigation;

import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Map;

public final class TreeNavigator {

    private TreeNavigator() {
    }

    public static void navigateTo(Component parent, Finding finding,
                                  Map<Finding, WeakReference<JMeterTreeNode>> navigation) {
        WeakReference<JMeterTreeNode> ref = navigation.get(finding);
        JMeterTreeNode node = (ref == null) ? null : ref.get();
        if (node == null || node.getParent() == null) {
            JOptionPane.showMessageDialog(parent,
                    "This element no longer exists. Rescan.",
                    "JMXAuditor",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        GuiPackage gp = GuiPackage.getInstance();
        if (gp == null) return;
        TreePath path = new TreePath(node.getPath());
        gp.getMainFrame().getTree().setSelectionPath(path);
        gp.getMainFrame().getTree().scrollPathToVisible(path);
    }
}
