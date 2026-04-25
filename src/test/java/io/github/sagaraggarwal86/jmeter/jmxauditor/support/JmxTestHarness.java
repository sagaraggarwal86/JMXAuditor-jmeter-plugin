package io.github.sagaraggarwal86.jmeter.jmxauditor.support;

import io.github.sagaraggarwal86.jmeter.jmxauditor.engine.Deadline;
import io.github.sagaraggarwal86.jmeter.jmxauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jmxauditor.engine.ScanStats;
import io.github.sagaraggarwal86.jmeter.jmxauditor.util.Clock;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.util.JMeterUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared test bootstrap. Initialises {@link JMeterUtils} with a minimal
 * {@code jmeter.properties} stub on the classpath, so {@link TestElement}
 * subclasses can be instantiated in unit tests without a live JMeter install.
 *
 * <p>Also provides factory helpers for building tree nodes and a
 * {@link ScanContext} pre-wired to a {@link FakeClock}.
 */
public final class JmxTestHarness {

    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    private JmxTestHarness() {
    }

    /**
     * Idempotent. Safe to call from every test method or {@code @BeforeAll}.
     */
    public static void init() {
        if (!INIT.compareAndSet(false, true)) return;
        // Locale.ROOT keeps resource-bundle lookups deterministic across CI machines.
        JMeterUtils.setJMeterHome(System.getProperty("java.io.tmpdir"));
        JMeterUtils.setLocale(Locale.ROOT);
        // JMeterUtils.loadJMeterProperties() expects a real file path (FileInputStream),
        // not a classpath resource. Write the bare-minimum properties to a temp file.
        try {
            Path tmp = Files.createTempFile("jmxauditor-jmeter", ".properties");
            tmp.toFile().deleteOnExit();
            Files.writeString(tmp, "language=en\nlog_level.jmeter=INFO\n");
            JMeterUtils.loadJMeterProperties(tmp.toString());
        } catch (IOException e) {
            throw new AssertionError("could not bootstrap JMeterUtils properties: " + e.getMessage(), e);
        }
    }

    /**
     * Build a {@link JMeterTreeModel} containing only a {@link TestPlan} root —
     * useful for whole-tree rules that walk the model from the root.
     */
    public static JMeterTreeModel emptyPlan() {
        init();
        TestPlan plan = new TestPlan("Test Plan");
        plan.setEnabled(true);
        return new JMeterTreeModel(plan);
    }

    /**
     * Wrap an arbitrary element in a tree of {@code TestPlan → element}, returning the
     * tree node corresponding to {@code element} so a per-node rule can be invoked on it.
     */
    public static NodeWithModel singleElement(TestElement element) {
        return planWithChildren(b -> b.add(element, 0));
    }

    /**
     * Build {@code TestPlan → arbitrary children} via a fluent builder. The returned
     * {@link NodeWithModel#node()} is the first added child (handy for per-node rules
     * targeting that element).
     */
    public static NodeWithModel planWithChildren(TreeSpec spec) {
        init();
        TestPlan plan = new TestPlan("Test Plan");
        plan.setEnabled(true);
        JMeterTreeModel model = new JMeterTreeModel(plan);
        JMeterTreeNode root = (JMeterTreeNode) model.getRoot();
        // model.getRoot() is the invisible synthetic root; child(0) is the real TestPlan node.
        JMeterTreeNode planNode = (JMeterTreeNode) root.getChildAt(0);
        Builder b = new Builder(model, planNode);
        try {
            spec.build(b);
        } catch (IllegalUserActionException e) {
            throw new AssertionError("tree spec failed: " + e.getMessage(), e);
        }
        if (b.first == null) {
            throw new AssertionError("TreeSpec did not add any children");
        }
        return new NodeWithModel(b.first, model);
    }

    /**
     * Build a {@link ScanContext} with a {@link FakeClock} at epoch and a 10 s deadline.
     */
    public static ScanContext newContext(JMeterTreeModel tree) {
        FakeClock clock = FakeClock.atEpoch();
        return new ScanContext(tree, new ScanStats(), new Deadline(clock, Duration.ofSeconds(10)), clock);
    }

    /**
     * Build a {@link ScanContext} with a custom {@link Clock} (e.g. an already-advanced
     * {@link FakeClock}, for testing deadline expiry).
     */
    public static ScanContext newContext(JMeterTreeModel tree, Clock clock) {
        return new ScanContext(tree, new ScanStats(), new Deadline(clock, Duration.ofSeconds(10)), clock);
    }

    /**
     * Functional interface for declaring a tree shape inside {@link #planWithChildren}.
     */
    @FunctionalInterface
    public interface TreeSpec {
        void build(Builder b) throws IllegalUserActionException;
    }

    /**
     * Wrap a {@link Builder} so a {@link TreeSpec} can declare a tree shape without
     * caller code having to thread the model+parent through every call.
     */
    public static final class Builder {
        private final JMeterTreeModel model;
        private final JMeterTreeNode parent;
        JMeterTreeNode first;

        Builder(JMeterTreeModel model, JMeterTreeNode parent) {
            this.model = model;
            this.parent = parent;
        }

        public JMeterTreeNode add(TestElement element) throws IllegalUserActionException {
            return add(element, -1);
        }

        public JMeterTreeNode add(TestElement element, int rememberAtIdx) throws IllegalUserActionException {
            JMeterTreeNode added = model.addComponent(element, parent);
            if (rememberAtIdx >= 0 && first == null) first = added;
            return added;
        }

        public Builder under(JMeterTreeNode newParent) {
            return new Builder(model, newParent);
        }

        public JMeterTreeModel model() {
            return model;
        }

        public JMeterTreeNode parent() {
            return parent;
        }
    }

    /**
     * Tuple of {@code (treeNode, model)} returned by {@link #singleElement} /
     * {@link #planWithChildren}.
     */
    public record NodeWithModel(JMeterTreeNode node, JMeterTreeModel model) {
    }
}
