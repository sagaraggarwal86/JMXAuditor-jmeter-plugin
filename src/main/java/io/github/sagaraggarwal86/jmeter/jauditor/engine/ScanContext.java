package io.github.sagaraggarwal86.jmeter.jauditor.engine;

import io.github.sagaraggarwal86.jmeter.jauditor.model.NodePath;
import io.github.sagaraggarwal86.jmeter.jauditor.util.Clock;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.*;
import java.util.function.Supplier;

/**
 * Per-scan shared state. Memo keys are lowercase noun phrases, no separators:
 * {@code httpSamplers}, {@code threadGroups}, {@code anyCookieManager}, {@code udvElements},
 * {@code headerManagers}, {@code assertionsByParent}, {@code timersByThreadGroup}.
 */
public final class ScanContext {

    private final JMeterTreeModel tree;
    private final ScanStats stats;
    private final Deadline deadline;
    private final Clock clock;
    private final Map<String, Object> memo = new HashMap<>();
    private final Map<JMeterTreeNode, NodePath> pathCache = new IdentityHashMap<>();
    private final Map<JMeterTreeNode, Map<Class<?>, Boolean>> descCache = new IdentityHashMap<>();

    public ScanContext(JMeterTreeModel tree, ScanStats stats, Deadline deadline, Clock clock) {
        this.tree = tree;
        this.stats = stats;
        this.deadline = deadline;
        this.clock = clock;
    }

    private static boolean computeDescendant(JMeterTreeNode node, Class<?> type) {
        Enumeration<?> en = node.children();
        while (en.hasMoreElements()) {
            Object c = en.nextElement();
            if (c instanceof JMeterTreeNode tn) {
                TestElement te = tn.getTestElement();
                if (te != null && type.isAssignableFrom(te.getClass())) return true;
                if (computeDescendant(tn, type)) return true;
            }
        }
        return false;
    }

    public JMeterTreeModel tree() {
        return tree;
    }

    public ScanStats stats() {
        return stats;
    }

    public Deadline deadline() {
        return deadline;
    }

    public Clock clock() {
        return clock;
    }

    @SuppressWarnings("unchecked")
    public <T> T memoize(String key, Supplier<T> compute) {
        return (T) memo.computeIfAbsent(key, k -> compute.get());
    }

    public NodePath pathFor(JMeterTreeNode node) {
        NodePath cached = pathCache.get(node);
        if (cached != null) return cached;
        List<String> segs = new ArrayList<>();
        Object[] parts = node.getPath();
        for (Object p : parts) {
            if (p instanceof JMeterTreeNode tn) segs.add(String.valueOf(tn.getName()));
        }
        if (segs.isEmpty()) segs.add(String.valueOf(node.getName()));
        NodePath np = new NodePath(segs);
        pathCache.put(node, np);
        return np;
    }

    public boolean hasDescendantOfType(JMeterTreeNode node, Class<?> type) {
        Map<Class<?>, Boolean> perNode = descCache.computeIfAbsent(node, k -> new HashMap<>());
        Boolean cached = perNode.get(type);
        if (cached != null) return cached;
        boolean found = computeDescendant(node, type);
        perNode.put(type, found);
        return found;
    }
}
