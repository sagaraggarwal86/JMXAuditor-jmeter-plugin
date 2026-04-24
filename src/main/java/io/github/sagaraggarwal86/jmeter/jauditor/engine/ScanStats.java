package io.github.sagaraggarwal86.jmeter.jauditor.engine;

/**
 * Mutable counters tracking a single scan's progress. Mutated only on the
 * {@code JAuditor-Scan} thread (invariant 8) so no synchronization is required.
 */
public final class ScanStats {
    private int nodesVisited;
    private int rulesExecuted;
    private int findingsEmitted;

    public int nodesVisited() {
        return nodesVisited;
    }

    public int rulesExecuted() {
        return rulesExecuted;
    }

    public int findingsEmitted() {
        return findingsEmitted;
    }

    public void incNodes() {
        nodesVisited++;
    }

    public void incRules() {
        rulesExecuted++;
    }

    public void incFindings(int n) {
        findingsEmitted += n;
    }
}
