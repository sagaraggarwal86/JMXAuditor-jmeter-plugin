package io.github.sagaraggarwal86.jmeter.jauditor.engine;

import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanOutcome;
import org.apache.jmeter.gui.tree.JMeterTreeNode;

import java.util.*;
import java.util.function.BiConsumer;

/** Iterative DFS with interrupt / deadline / cap checks at every node boundary. */
public final class TreeWalker {

    private TreeWalker() {
    }

    public static WalkResult walk(JMeterTreeNode root, ScanContext ctx, BiConsumer<JMeterTreeNode, Integer> visitor) {
        Deque<JMeterTreeNode> stack = new ArrayDeque<>();
        stack.push(root);
        int visited = 0;

        while (!stack.isEmpty()) {
            if (Thread.interrupted()) return new WalkResult(visited, AbortReason.CANCELLED);
            if (ctx.deadline().expired()) return new WalkResult(visited, AbortReason.TIMEOUT);
            if (visited >= ScanLimits.MAX_NODES) return new WalkResult(visited, AbortReason.NODE_LIMIT);
            if (ctx.stats().findingsEmitted() >= ScanLimits.MAX_FINDINGS)
                return new WalkResult(visited, AbortReason.FINDING_LIMIT);

            JMeterTreeNode node = stack.pop();
            visitor.accept(node, visited);
            ctx.stats().incNodes();
            visited++;

            List<JMeterTreeNode> kids = new ArrayList<>();
            Enumeration<?> en = node.children();
            while (en.hasMoreElements()) {
                Object c = en.nextElement();
                if (c instanceof JMeterTreeNode tn) kids.add(tn);
            }
            for (int i = kids.size() - 1; i >= 0; i--) stack.push(kids.get(i));
        }
        return new WalkResult(visited, AbortReason.NONE);
    }

    public static ScanOutcome mapOutcome(AbortReason r) {
        return switch (r) {
            case NONE -> ScanOutcome.COMPLETE;
            case CANCELLED -> ScanOutcome.CANCELLED;
            case TIMEOUT -> ScanOutcome.TIMEOUT;
            case NODE_LIMIT -> ScanOutcome.NODE_LIMIT;
            case FINDING_LIMIT -> ScanOutcome.FINDING_LIMIT;
        };
    }

    public enum AbortReason {NONE, CANCELLED, TIMEOUT, NODE_LIMIT, FINDING_LIMIT}

    public record WalkResult(int nodesVisited, AbortReason abortReason) {
    }
}
