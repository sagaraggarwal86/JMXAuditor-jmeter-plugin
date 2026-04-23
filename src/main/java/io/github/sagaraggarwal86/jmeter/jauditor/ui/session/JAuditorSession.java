package io.github.sagaraggarwal86.jmeter.jauditor.ui.session;

import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jauditor.util.EdtAssertions;

import java.awt.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class JAuditorSession {

    private static final JAuditorSession INSTANCE = new JAuditorSession();
    private final Set<String> hiddenRuleIds = new HashSet<>();
    private Dimension dialogSize;
    private Point dialogLocation;
    private Path lastHtmlExportDir;
    private Path lastJsonExportDir;
    private boolean suppressUnsavedPrompt;
    private ScanResult currentFindings;

    private JAuditorSession() {
    }

    public static JAuditorSession get() {
        return INSTANCE;
    }

    public Dimension dialogSize() {
        return dialogSize;
    }

    public void setDialogSize(Dimension d) {
        EdtAssertions.assertEdt();
        this.dialogSize = d;
    }

    public Point dialogLocation() {
        return dialogLocation;
    }

    public void setDialogLocation(Point p) {
        EdtAssertions.assertEdt();
        this.dialogLocation = p;
    }

    public Path lastHtmlExportDir() {
        return lastHtmlExportDir;
    }

    public void setLastHtmlExportDir(Path p) {
        EdtAssertions.assertEdt();
        this.lastHtmlExportDir = p;
    }

    public Path lastJsonExportDir() {
        return lastJsonExportDir;
    }

    public void setLastJsonExportDir(Path p) {
        EdtAssertions.assertEdt();
        this.lastJsonExportDir = p;
    }

    public boolean suppressUnsavedPrompt() {
        return suppressUnsavedPrompt;
    }

    public void setSuppressUnsavedPrompt(boolean v) {
        EdtAssertions.assertEdt();
        this.suppressUnsavedPrompt = v;
    }

    public Set<String> hiddenRuleIds() {
        return hiddenRuleIds;
    }

    public ScanResult currentFindings() {
        return currentFindings;
    }

    public void setCurrentFindings(ScanResult r) {
        EdtAssertions.assertEdt();
        this.currentFindings = r;
    }

    public void clearOnDialogClose() {
        this.currentFindings = null;
    }

    void reset() {
        dialogSize = null;
        dialogLocation = null;
        lastHtmlExportDir = null;
        lastJsonExportDir = null;
        suppressUnsavedPrompt = false;
        hiddenRuleIds.clear();
        currentFindings = null;
    }
}
