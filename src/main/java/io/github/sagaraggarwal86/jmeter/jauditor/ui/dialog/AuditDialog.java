package io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog;

import io.github.sagaraggarwal86.jmeter.jauditor.JAuditorPlugin;
import io.github.sagaraggarwal86.jmeter.jauditor.export.html.HtmlReportWriter;
import io.github.sagaraggarwal86.jmeter.jauditor.export.json.JsonReportWriter;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.RuleRegistry;
import io.github.sagaraggarwal86.jmeter.jauditor.scan.ScanWorker;
import io.github.sagaraggarwal86.jmeter.jauditor.ui.navigation.TreeNavigator;
import io.github.sagaraggarwal86.jmeter.jauditor.ui.session.JAuditorSession;
import io.github.sagaraggarwal86.jmeter.jauditor.util.JAuditorLog;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.util.JMeterUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public final class AuditDialog extends JDialog {

    private static final JAuditorLog LOG = JAuditorLog.forClass(AuditDialog.class);
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(java.time.ZoneId.systemDefault());
    private final HeaderBar header;
    private final KpiCardPanel kpi;
    private final SeverityTabs tabs;
    private final FindingsTableModel model;
    private final JTable table;
    private final FooterBar footer;
    private final JLabel banner;
    private DialogState state = DialogState.IDLE;
    private ScanWorker worker;
    private ScanResult lastResult;

    public AuditDialog(JFrame owner) {
        super(owner, JAuditorPlugin.NAME, false);
        setMinimumSize(new Dimension(600, 400));
        Dimension size = JAuditorSession.get().dialogSize();
        setSize(size != null ? size : new Dimension(900, 600));
        if (JAuditorSession.get().dialogLocation() != null) setLocation(JAuditorSession.get().dialogLocation());
        else setLocationRelativeTo(owner);

        header = new HeaderBar(this::onRescan, this::onCancel, this::onExportHtml, this::onExportJson);
        kpi = new KpiCardPanel();
        model = new FindingsTableModel();
        tabs = new SeverityTabs();
        tabs.setFilterListener(f -> {
            model.setFilter(f);
            tabs.updateCounts(model);
        });
        table = new JTable(model);
        table.setTableHeader(null);
        table.setShowGrid(false);
        table.getColumnModel().getColumn(0).setCellRenderer(new FindingsTableRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeContext(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeContext(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) navigateSelected();
            }
        });
        footer = new FooterBar(pluginVersion(), this::onShowAll);
        banner = new JLabel("");
        banner.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        banner.setVisible(false);

        JPanel top = new JPanel(new BorderLayout());
        top.add(header, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout());
        mid.add(kpi, BorderLayout.NORTH);
        mid.add(banner, BorderLayout.CENTER);
        mid.add(tabs, BorderLayout.SOUTH);
        top.add(mid, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        bindKeys();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                JAuditorSession.get().setDialogSize(getSize());
                JAuditorSession.get().setDialogLocation(getLocation());
                JAuditorSession.get().clearOnDialogClose();
            }
        });
    }

    private static java.awt.Toolkit Toolkit() {
        return java.awt.Toolkit.getDefaultToolkit();
    }

    private static String ts() {
        return FILE_TS.format(Instant.now());
    }

    private static String pluginVersion() {
        return io.github.sagaraggarwal86.jmeter.jauditor.JAuditorPlugin.version();
    }

    public void startScan() {
        if (state == DialogState.SCANNING || state == DialogState.CANCELLING) return;
        GuiPackage gp = GuiPackage.getInstance();
        if (gp == null) return;
        JMeterTreeModel tree = gp.getTreeModel();
        boolean dirty = gp.isDirty();
        if (dirty && !JAuditorSession.get().suppressUnsavedPrompt()) {
            int rc = JOptionPane.showConfirmDialog(this,
                    "Unsaved changes detected — audit the in-memory state?",
                    JAuditorPlugin.NAME, JOptionPane.YES_NO_CANCEL_OPTION);
            if (rc == JOptionPane.CANCEL_OPTION || rc == JOptionPane.CLOSED_OPTION) return;
        }

        String path = gp.getTestPlanFile();
        String name = (path == null) ? "untitled" : new File(path).getName();
        header.setTitleText(name, dirty, path == null);

        setState(DialogState.SCANNING);
        model.setFindings(java.util.List.of());
        tabs.resetToAll();
        tabs.updateCounts(model);
        kpi.update(java.util.List.of());
        banner.setVisible(false);

        worker = new ScanWorker(tree, JAuditorSession.get().hiddenRuleIds(), path, name,
                JMeterUtils.getJMeterVersion(), dirty, null);
        worker.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName())
                    && evt.getNewValue() == javax.swing.SwingWorker.StateValue.DONE) {
                onWorkerDone();
            }
        });
        worker.execute();
    }

    private void onWorkerDone() {
        if (worker.isCancelled()) {
            setState(DialogState.IDLE);
            model.setFindings(java.util.List.of());
            tabs.updateCounts(model);
            return;
        }
        try {
            ScanResult r = worker.get();
            lastResult = r;
            JAuditorSession.get().setCurrentFindings(r);
            model.setFindings(r.findings());
            tabs.updateCounts(model);
            kpi.update(r.findings());
            footer.setMetadata(Math.max(0, r.durationMs() / 1000L), r.nodesAnalyzed(), RuleRegistry.count());
            footer.setHiddenCount(JAuditorSession.get().hiddenRuleIds().size());
            if (r.outcome().isTruncated()) {
                banner.setText(r.outcome().bannerMessage(r.findings().size()));
                banner.setVisible(true);
            } else {
                banner.setVisible(false);
            }
            setState(DialogState.DONE);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            setState(DialogState.IDLE);
        } catch (ExecutionException ee) {
            LOG.error("scan failed", ee.getCause());
            JOptionPane.showMessageDialog(this,
                    "An unexpected error occurred. Check jmeter.log.",
                    JAuditorPlugin.NAME, JOptionPane.ERROR_MESSAGE);
            setState(DialogState.IDLE);
        }
    }

    private void setState(DialogState s) {
        this.state = s;
        header.applyState(s, lastResult != null && !lastResult.findings().isEmpty());
    }

    private void onRescan() {
        startScan();
    }

    private void onCancel() {
        if (worker != null && state == DialogState.SCANNING) {
            setState(DialogState.CANCELLING);
            worker.cancel(true);
        }
    }

    private void onShowAll() {
        JAuditorSession.get().hiddenRuleIds().clear();
        footer.setHiddenCount(0);
        startScan();
    }

    private void onExportHtml() {
        doExport("jauditor-report-", ".html",
                JAuditorSession.get()::lastHtmlExportDir,
                JAuditorSession.get()::setLastHtmlExportDir,
                out -> HtmlReportWriter.write(lastResult, pluginVersion(), out));
    }

    private void onExportJson() {
        doExport("jauditor-findings-", ".json",
                JAuditorSession.get()::lastJsonExportDir,
                JAuditorSession.get()::setLastJsonExportDir,
                out -> JsonReportWriter.write(lastResult, pluginVersion(), out));
    }

    private void doExport(String prefix, String ext,
                          java.util.function.Supplier<java.nio.file.Path> getLastDir,
                          java.util.function.Consumer<java.nio.file.Path> setLastDir,
                          ExportAction writer) {
        if (lastResult == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(prefix + baseName() + "-" + ts() + ext));
        if (getLastDir.get() != null) fc.setCurrentDirectory(getLastDir.get().toFile());
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();
        try {
            writer.run(out.toPath());
            setLastDir.accept(out.getParentFile().toPath());
        } catch (Exception ex) {
            LOG.error("export failed", ex);
            JOptionPane.showMessageDialog(this, "Export failed. Check jmeter.log.",
                    JAuditorPlugin.NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void maybeContext(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int row = table.rowAtPoint(e.getPoint());
        if (row < 0) return;
        table.getSelectionModel().setSelectionInterval(row, row);
        Finding f = model.at(row);
        FindingsContextMenu.build(f,
                this::navigateTo,
                ruleId -> {
                    JAuditorSession.get().hiddenRuleIds().add(ruleId);
                    footer.setHiddenCount(JAuditorSession.get().hiddenRuleIds().size());
                }).show(e.getComponent(), e.getX(), e.getY());
    }

    private void navigateSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        navigateTo(model.at(row));
    }

    private void navigateTo(Finding f) {
        if (lastResult == null) return;
        Map<Finding, WeakReference<JMeterTreeNode>> nav = lastResult.navigation();
        TreeNavigator.navigateTo(this, f, nav);
    }

    private void bindKeys() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        am.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "rescan");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit().getMenuShortcutKeyMaskEx()), "rescan");
        am.put("rescan", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onRescan();
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "navigate");
        am.put("navigate", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateSelected();
            }
        });
    }

    private String baseName() {
        if (lastResult == null || lastResult.jmxFileName() == null) return "untitled";
        String n = lastResult.jmxFileName();
        int dot = n.lastIndexOf('.');
        return (dot > 0) ? n.substring(0, dot) : n;
    }

    @FunctionalInterface
    private interface ExportAction {
        void run(java.nio.file.Path out) throws Exception;
    }
}
