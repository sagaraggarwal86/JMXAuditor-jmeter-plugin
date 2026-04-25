package io.github.sagaraggarwal86.jmeter.jmxauditor.ui.action;

import io.github.sagaraggarwal86.jmeter.jmxauditor.JMXAuditorPlugin;
import io.github.sagaraggarwal86.jmeter.jmxauditor.util.JMXAuditorLog;
import org.apache.jmeter.gui.plugin.MenuCreator;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public final class JMXAuditorMenuCreator implements MenuCreator {

    private static final JMXAuditorLog LOG = JMXAuditorLog.forClass(JMXAuditorMenuCreator.class);
    private static final AuditCommand COMMAND = new AuditCommand();
    private static boolean initialized;

    public JMXAuditorMenuCreator() {
        try {
            bootstrap();
        } catch (Throwable t) {
            LOG.error("initialization failed", t);
        }
    }

    private static synchronized void bootstrap() {
        if (initialized) return;
        ToolbarButtonInstaller.installAsync();
        initialized = true;
    }

    static AuditCommand command() {
        return COMMAND;
    }

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
        if (location != MENU_LOCATION.TOOLS) return new JMenuItem[0];
        JMenuItem item = new JMenuItem("Audit Script");
        int mask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK;
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, mask));
        item.setActionCommand(JMXAuditorPlugin.ACTION_ID);
        item.addActionListener(e -> COMMAND.doAction(
                new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, JMXAuditorPlugin.ACTION_ID)));
        return new JMenuItem[]{item};
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menu) {
        return false;
    }

    @Override
    public void localeChanged() { /* no-op */ }
}
