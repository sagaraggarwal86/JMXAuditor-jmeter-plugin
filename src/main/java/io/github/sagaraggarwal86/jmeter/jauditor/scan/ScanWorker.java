package io.github.sagaraggarwal86.jmeter.jauditor.scan;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.RuleEngine;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jauditor.util.Clock;
import org.apache.jmeter.gui.tree.JMeterTreeModel;

import javax.swing.*;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class ScanWorker extends SwingWorker<ScanResult, Finding> {

    private final JMeterTreeModel tree;
    private final Set<String> hiddenRuleIds;
    private final String jmxPath;
    private final String jmxName;
    private final String jmeterVersion;
    private final boolean dirty;
    private final Consumer<Finding> progress;

    public ScanWorker(JMeterTreeModel tree, Set<String> hiddenRuleIds, String jmxPath, String jmxName,
                      String jmeterVersion, boolean dirty, Consumer<Finding> progress) {
        this.tree = tree;
        this.hiddenRuleIds = hiddenRuleIds;
        this.jmxPath = jmxPath;
        this.jmxName = jmxName;
        this.jmeterVersion = jmeterVersion;
        this.dirty = dirty;
        this.progress = progress;
    }

    @Override
    protected ScanResult doInBackground() {
        Thread.currentThread().setName("JAuditor-Scan");
        return RuleEngine.scan(tree, hiddenRuleIds, jmxPath, jmxName, jmeterVersion, dirty, Clock.system(), this::publish);
    }

    @Override
    protected void process(List<Finding> chunks) {
        if (progress == null) return;
        for (Finding f : chunks) progress.accept(f);
    }
}
