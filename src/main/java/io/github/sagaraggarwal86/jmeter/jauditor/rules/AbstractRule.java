package io.github.sagaraggarwal86.jmeter.jauditor.rules;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.NodePath;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Shared helpers for reading JMeter properties ({@link #propString}, {@link #propBool},
 * {@link #propInt}), detecting JMeter variable references ({@link #hasJMeterVar}),
 * walking the whole tree ({@link #allNodes}), and constructing findings
 * ({@link #make}). Rules subclass this rather than implement {@link Rule} directly.
 * All helpers treat missing properties as empty/default — {@link #propString} in
 * particular is guaranteed non-null, which is why the rules do not null-check its
 * return value.
 */
public abstract class AbstractRule implements Rule {

    protected static String propString(TestElement te, String key) {
        if (te == null) return "";
        JMeterProperty p = te.getProperty(key);
        if (p == null) return "";
        String v = p.getStringValue();
        return v == null ? "" : v;
    }

    protected static boolean propBlank(TestElement te, String key) {
        return propString(te, key).isBlank();
    }

    /**
     * True if {@code s} contains a JMeter variable reference ({@code ${...}} or {@code ${__func(...)}}) — treat as non-literal.
     */
    protected static boolean hasJMeterVar(String s) {
        return s != null && s.contains("${");
    }

    protected static boolean propBool(TestElement te, String key) {
        if (te == null) return false;
        JMeterProperty p = te.getProperty(key);
        return p != null && Boolean.parseBoolean(p.getStringValue());
    }

    protected static int propInt(TestElement te, String key, int def) {
        String s = propString(te, key);
        if (s.isBlank()) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    protected static List<JMeterTreeNode> allNodes(JMeterTreeModel tree) {
        List<JMeterTreeNode> out = new ArrayList<>();
        JMeterTreeNode root = (JMeterTreeNode) tree.getRoot();
        collect(root, out);
        return out;
    }

    private static void collect(JMeterTreeNode node, List<JMeterTreeNode> out) {
        out.add(node);
        Enumeration<?> en = node.children();
        while (en.hasMoreElements()) {
            Object c = en.nextElement();
            if (c instanceof JMeterTreeNode tn) collect(tn, out);
        }
    }

    protected Finding make(NodePath path, String title, String description, String suggestion) {
        return new Finding(id(), category(), severity(), title, description, suggestion, path, path.depth());
    }
}
