package io.github.sagaraggarwal86.jmeter.jauditor.rules.scalability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class GuiListenerInLoadPathRule extends AbstractRule {

    private static final Set<String> GUI_HEAVY = Set.of(
            "ViewResultsFullVisualizer",
            "TableVisualizer",
            "GraphVisualizer",
            "StatVisualizer",
            "SummaryReport",
            "AssertionVisualizer",
            "RespTimeGraphVisualizer",
            "DistributionGraphVisualizer"
    );

    @Override
    public String id() {
        return "GUI_LISTENER_IN_LOAD_PATH";
    }

    @Override
    public Category category() {
        return Category.SCALABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.ERROR;
    }

    @Override
    public String description() {
        return "GUI-heavy ResultCollector enabled on the load path (memory blow-up risk).";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(ResultCollector.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        if (!te.isEnabled()) return List.of();
        String guiClass = te.getPropertyAsString("TestElement.gui_class");
        if (guiClass == null) return List.of();
        String simple = guiClass.substring(guiClass.lastIndexOf('.') + 1);
        if (!GUI_HEAVY.contains(simple)) return List.of();
        return List.of(make(ctx.pathFor(node),
                "GUI-heavy listener enabled on load path",
                "Listener '" + simple + "' accumulates results in memory and is not safe for long or high-volume runs.",
                "Disable in GUI-mode runs or replace with Simple Data Writer + offline analysis."));
    }
}
