package io.github.sagaraggarwal86.jmeter.jmxauditor.ui.dialog;

import io.github.sagaraggarwal86.jmeter.jmxauditor.JMXAuditorPlugin;
import io.github.sagaraggarwal86.jmeter.jmxauditor.export.html.HtmlReportWriter;
import io.github.sagaraggarwal86.jmeter.jmxauditor.export.json.JsonReportWriter;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jmxauditor.rules.RuleRegistry;
import io.github.sagaraggarwal86.jmeter.jmxauditor.scan.ScanWorker;
import io.github.sagaraggarwal86.jmeter.jmxauditor.ui.navigation.TreeNavigator;
import io.github.sagaraggarwal86.jmeter.jmxauditor.ui.session.JMXAuditorSession;
import io.github.sagaraggarwal86.jmeter.jmxauditor.util.JMXAuditorLog;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.util.JMeterUtils;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public final class AuditDialog extends JDialog {

    private static final JMXAuditorLog LOG = JMXAuditorLog.forClass(AuditDialog.class);
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(java.time.ZoneId.systemDefault());
    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_EMPTY = "EMPTY";
    private final HeaderBar header;
    private final KpiCardPanel kpi;
    private final SeverityTabs tabs;
    private final FindingsTableModel model;
    private final JTable table;
    private final FooterBar footer;
    private final JLabel banner;
    private final JLabel emptyMessage;
    private final CardLayout centerCards;
    private final JPanel centerPanel;
    private final int originalTooltipDismissDelay;
    private DialogState state = DialogState.IDLE;
    private ScanWorker worker;
    private ScanResult lastResult;

    public AuditDialog(JFrame owner) {
        super(owner, JMXAuditorPlugin.NAME, false);
        setMinimumSize(new Dimension(600, 400));
        Dimension size = JMXAuditorSession.get().dialogSize();
        setSize(size != null ? size : new Dimension(1000, 600));
        if (JMXAuditorSession.get().dialogLocation() != null) setLocation(JMXAuditorSession.get().dialogLocation());
        else setLocationRelativeTo(owner);

        header = new HeaderBar(this::onRescan, this::onCancel, this::onExportHtml, this::onExportJson);
        kpi = new KpiCardPanel();
        model = new FindingsTableModel();
        tabs = new SeverityTabs();
        tabs.setFilterListener(f -> {
            model.setFilter(f);
            tabs.updateCounts(model);
            updateEmptyState();
        });
        kpi.setSelectionListener(cats -> {
            model.setAllowedCategories(cats);
            tabs.updateCounts(model);
            updateEmptyState();
        });
        table = new JTable(model);
        table.setTableHeader(null);
        table.setShowGrid(false);
        table.setToolTipText("Double-click or Enter to navigate to the element");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setCellRenderer(new FindingsTableRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(1200);
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateEmptyState();
            }
        });
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

        emptyMessage = new JLabel("Click Rescan to audit the current test plan.", SwingConstants.CENTER);
        emptyMessage.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        Font base = UIManager.getFont("Label.font");
        if (base != null) emptyMessage.setFont(base);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        centerCards = new CardLayout();
        centerPanel = new JPanel(centerCards);
        centerPanel.add(tableScroll, CARD_TABLE);
        centerPanel.add(emptyMessage, CARD_EMPTY);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(kpi, BorderLayout.CENTER);
        topRow.add(header, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout());
        top.add(topRow, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout());
        mid.add(banner, BorderLayout.NORTH);
        mid.add(tabs, BorderLayout.SOUTH);
        top.add(mid, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        bindKeys();
        updateEmptyState();
        originalTooltipDismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
        ToolTipManager.sharedInstance().setDismissDelay(60_000);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                JMXAuditorSession.get().setDialogSize(getSize());
                JMXAuditorSession.get().setDialogLocation(getLocation());
                JMXAuditorSession.get().clearOnDialogClose();
                ToolTipManager.sharedInstance().setDismissDelay(originalTooltipDismissDelay);
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
        return io.github.sagaraggarwal86.jmeter.jmxauditor.JMXAuditorPlugin.version();
    }

    private void applyWindowTitle(String jmxName, boolean dirty) {
        if (jmxName == null) {
            setTitle(JMXAuditorPlugin.NAME);
        } else {
            setTitle(JMXAuditorPlugin.NAME + "-" + jmxName + (dirty ? " •" : ""));
        }
    }

    public void startScan() {
        if (state == DialogState.SCANNING || state == DialogState.CANCELLING) return;
        GuiPackage gp = GuiPackage.getInstance();
        if (gp == null) return;
        JMeterTreeModel tree = gp.getTreeModel();
        boolean dirty = gp.isDirty();
        if (dirty && !JMXAuditorSession.get().suppressUnsavedPrompt()) {
            int rc = JOptionPane.showConfirmDialog(this,
                    "Unsaved changes detected — audit the in-memory state?",
                    JMXAuditorPlugin.NAME, JOptionPane.YES_NO_CANCEL_OPTION);
            if (rc == JOptionPane.CANCEL_OPTION || rc == JOptionPane.CLOSED_OPTION) return;
        }

        String path = gp.getTestPlanFile();
        String name = (path == null) ? "untitled" : new File(path).getName();
        applyWindowTitle(path == null ? null : name, dirty);

        setState(DialogState.SCANNING);
        model.setFindings(java.util.List.of());
        tabs.resetToAll();
        tabs.updateCounts(model);
        kpi.update(java.util.List.of());
        kpi.resetSelection();
        banner.setVisible(false);

        worker = new ScanWorker(tree, JMXAuditorSession.get().hiddenRuleIds(), path, name,
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
            JMXAuditorSession.get().setCurrentFindings(r);
            model.setFindings(r.findings());
            updateColumnWidth(r.findings());
            tabs.updateCounts(model);
            kpi.update(r.findings());
            footer.setMetadata(Math.max(0L, r.durationMs()), r.nodesAnalyzed(), RuleRegistry.count());
            footer.setHiddenCount(JMXAuditorSession.get().hiddenRuleIds().size());
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
                    JMXAuditorPlugin.NAME, JOptionPane.ERROR_MESSAGE);
            setState(DialogState.IDLE);
        }
    }

    private void updateColumnWidth(java.util.List<Finding> findings) {
        if (findings.isEmpty()) return;
        Font base = UIManager.getFont("Label.font");
        FontMetrics fm = getFontMetrics(base != null ? base : table.getFont());
        int max = 400;
        for (Finding f : findings) {
            int titleW = fm.stringWidth("[" + f.category().name() + "] " + f.title());
            int pathW = fm.stringWidth(f.nodePath().breadcrumb());
            int sugW = (f.suggestion() != null) ? fm.stringWidth(f.suggestion()) : 0;
            int rowMax = Math.max(Math.max(titleW, pathW), sugW);
            if (rowMax > max) max = rowMax;
        }
        // stripe border (3) + text panel border (10 + 10) + small safety
        max += 40;
        table.getColumnModel().getColumn(0).setPreferredWidth(max);
        table.revalidate();
    }

    private void setState(DialogState s) {
        this.state = s;
        header.applyState(s, lastResult != null && !lastResult.findings().isEmpty());
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (model.getRowCount() > 0) {
            centerCards.show(centerPanel, CARD_TABLE);
            return;
        }
        String msg;
        if (state == DialogState.SCANNING) {
            msg = "Scanning…";
        } else if (state == DialogState.CANCELLING) {
            msg = "Cancelling…";
        } else if (lastResult == null) {
            msg = "Click Rescan to audit the current test plan.";
        } else if (lastResult.findings().isEmpty()) {
            msg = "No findings. Your test plan passed all 25 audit rules.";
        } else {
            msg = "No findings match the current filter. Press 1 to show all severities, or re-enable categories above.";
        }
        emptyMessage.setText(msg);
        centerCards.show(centerPanel, CARD_EMPTY);
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
        JMXAuditorSession.get().clearHiddenRules();
        footer.setHiddenCount(0);
        startScan();
    }

    private void onExportHtml() {
        File saved = doExport("jmxauditor-report-", ".html",
                JMXAuditorSession.get()::lastHtmlExportDir,
                JMXAuditorSession.get()::setLastHtmlExportDir,
                out -> HtmlReportWriter.write(lastResult, pluginVersion(), out));
        if (saved != null) openInDefaultApp(saved);
    }

    private void onExportJson() {
        doExport("jmxauditor-report-", ".json",
                JMXAuditorSession.get()::lastJsonExportDir,
                JMXAuditorSession.get()::setLastJsonExportDir,
                out -> JsonReportWriter.write(lastResult, pluginVersion(), out));
    }

    private File doExport(String prefix, String ext,
                          java.util.function.Supplier<java.nio.file.Path> getLastDir,
                          java.util.function.Consumer<java.nio.file.Path> setLastDir,
                          ExportAction writer) {
        if (lastResult == null) return null;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(prefix + ts() + ext));
        if (getLastDir.get() != null) fc.setCurrentDirectory(getLastDir.get().toFile());
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        File out = fc.getSelectedFile();
        try {
            writer.run(out.toPath());
            setLastDir.accept(out.getParentFile().toPath());
            return out;
        } catch (Exception ex) {
            LOG.error("export failed", ex);
            JOptionPane.showMessageDialog(this, "Export failed. Check jmeter.log.",
                    JMXAuditorPlugin.NAME, JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void openInDefaultApp(File file) {
        if (!Desktop.isDesktopSupported()) return;
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) return;
        try {
            desktop.open(file);
        } catch (IOException ex) {
            LOG.warn("could not open report after save", ex);
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
                    JMXAuditorSession.get().addHiddenRule(ruleId);
                    footer.setHiddenCount(JMXAuditorSession.get().hiddenRuleIds().size());
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
        bindSeverity(im, am, KeyEvent.VK_1, "sevAll", FindingsTableModel.Filter.ALL);
        bindSeverity(im, am, KeyEvent.VK_2, "sevError", FindingsTableModel.Filter.ERROR);
        bindSeverity(im, am, KeyEvent.VK_3, "sevWarn", FindingsTableModel.Filter.WARN);
        bindSeverity(im, am, KeyEvent.VK_4, "sevInfo", FindingsTableModel.Filter.INFO);
        int[] catKeys = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3,
                KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6};
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            bindCategory(im, am, catKeys[i], "cat" + i, cats[i]);
        }
    }

    private void bindSeverity(InputMap im, ActionMap am, int keyCode, String name,
                              FindingsTableModel.Filter f) {
        im.put(KeyStroke.getKeyStroke(keyCode, 0), name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                tabs.selectFilter(f);
            }
        });
    }

    private void bindCategory(InputMap im, ActionMap am, int keyCode, String name, Category c) {
        im.put(KeyStroke.getKeyStroke(keyCode, InputEvent.ALT_DOWN_MASK), name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                kpi.toggleCategory(c);
            }
        });
    }

    @FunctionalInterface
    private interface ExportAction {
        void run(java.nio.file.Path out) throws Exception;
    }
}
