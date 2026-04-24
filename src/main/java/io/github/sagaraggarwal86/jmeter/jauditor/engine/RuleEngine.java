package io.github.sagaraggarwal86.jmeter.jauditor.engine;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.NodePath;
import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanOutcome;
import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.Rule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.RuleRegistry;
import io.github.sagaraggarwal86.jmeter.jauditor.util.Clock;
import io.github.sagaraggarwal86.jmeter.jauditor.util.JAuditorLog;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * Static entry point for running the 25-rule catalogue against a {@link JMeterTreeModel}.
 * Amortizes {@code isAssignableFrom} by caching {@code concrete class → matching rules}
 * in an {@link IdentityHashMap}. Implements the Tier 1 exception boundary: any
 * {@link Exception} thrown by a rule is logged and replaced with a synthetic INFO
 * finding via {@link io.github.sagaraggarwal86.jmeter.jauditor.model.Finding#ruleFailure}
 * — see CLAUDE.md "Exception topology" for the full picture.
 */
public final class RuleEngine {

    private static final JAuditorLog LOG = JAuditorLog.forClass(RuleEngine.class);

    // The one rule that must still run on disabled nodes — it exists to flag them.
    private static final String DISABLED_RULE_ID = "DISABLED_ELEMENT_IN_TREE";

    private RuleEngine() {
    }

    // True iff the node and every ancestor up to (but not including) the synthetic root has enabled=true.
    // JMeter's TestCompiler skips any subtree rooted at a disabled element, so children of a disabled
    // ancestor don't execute regardless of their own flag; rules should see the same effective reality.
    private static boolean effectivelyEnabled(JMeterTreeNode node) {
        for (JMeterTreeNode cur = node; cur != null && cur.getParent() != null; cur = (JMeterTreeNode) cur.getParent()) {
            TestElement te = cur.getTestElement();
            if (te != null && !te.isEnabled()) return false;
        }
        return true;
    }

    public static ScanResult scan(
            JMeterTreeModel tree,
            Set<String> hiddenRuleIds,
            String jmxPath,
            String jmxName,
            String jmeterVersion,
            boolean dirty,
            Clock clock,
            Consumer<Finding> publish) {

        Instant start = clock.now();
        ScanStats stats = new ScanStats();
        Deadline deadline = new Deadline(clock, Duration.ofMillis(ScanLimits.MAX_SCAN_MILLIS));
        ScanContext ctx = new ScanContext(tree, stats, deadline, clock);

        List<Rule> allRules = RuleRegistry.all();
        List<Rule> active = new ArrayList<>();
        List<String> suppressed = new ArrayList<>();
        for (Rule r : allRules) {
            if (hiddenRuleIds != null && hiddenRuleIds.contains(r.id())) {
                suppressed.add(r.id());
            } else {
                active.add(r);
            }
        }

        List<Finding> findings = new ArrayList<>();
        Map<Finding, WeakReference<JMeterTreeNode>> nav = new IdentityHashMap<>();
        JMeterTreeNode root = (JMeterTreeNode) tree.getRoot();
        // JMeterTreeModel roots the tree at a synthetic TestPlan node whose first child is the real TestPlan
        // (see JMeterTreeModel#initTree). Walking from the synthetic root would fire TestPlan-applicable rules
        // twice and leave the root-side finding un-navigable because root.getParent() == null.
        JMeterTreeNode startNode = root.getChildCount() > 0 ? (JMeterTreeNode) root.getChildAt(0) : root;

        Map<Class<?>, List<Rule>> rulesByConcrete = new IdentityHashMap<>();

        TreeWalker.WalkResult walk = TreeWalker.walk(startNode, ctx, (node, idx) -> {
            TestElement te = node.getTestElement();
            if (te == null) return;
            boolean enabled = effectivelyEnabled(node);
            List<Rule> matched = rulesByConcrete.computeIfAbsent(te.getClass(),
                    cls -> filterMatching(active, cls));
            for (Rule r : matched) {
                if (!enabled && !DISABLED_RULE_ID.equals(r.id())) continue;
                try {
                    List<Finding> fs = r.check(node, ctx);
                    ctx.stats().incRules();
                    if (fs == null || fs.isEmpty()) continue;
                    for (Finding f : fs) {
                        if (findings.size() >= ScanLimits.MAX_FINDINGS) break;
                        findings.add(f);
                        nav.put(f, new WeakReference<>(node));
                        if (publish != null) publish.accept(f);
                    }
                    ctx.stats().incFindings(fs.size());
                } catch (Exception ex) {
                    LOG.warn("rule {} failed on node {}: {}", r.id(), node.getName(), ex.toString());
                    NodePath path = ctx.pathFor(node);
                    Finding f = Finding.ruleFailure(r.id(), r.category(), path, ex);
                    findings.add(f);
                    nav.put(f, new WeakReference<>(node));
                    if (publish != null) publish.accept(f);
                }
            }
        });

        long duration = Duration.between(start, clock.now()).toMillis();
        ScanOutcome outcome = TreeWalker.mapOutcome(walk.abortReason());
        LOG.logScanSummary(findings.size(), stats.nodesVisited(), duration, outcome.name());

        return new ScanResult(
                start,
                jmxPath,
                jmxName,
                jmeterVersion,
                dirty,
                stats.nodesVisited(),
                stats.rulesExecuted(),
                duration,
                findings,
                outcome,
                suppressed,
                nav
        );
    }

    private static List<Rule> filterMatching(List<Rule> all, Class<?> concrete) {
        List<Rule> out = new ArrayList<>();
        for (Rule r : all) {
            Set<Class<? extends TestElement>> types = r.appliesTo();
            if (types == null || types.isEmpty()) {
                out.add(r);
                continue;
            }
            for (Class<? extends TestElement> t : types) {
                if (t.isAssignableFrom(concrete)) {
                    out.add(r);
                    break;
                }
            }
        }
        return out;
    }
}
