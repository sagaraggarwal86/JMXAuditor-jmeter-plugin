package io.github.sagaraggarwal86.jmeter.jmxauditor.scan;

import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanOutcome;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ScanWorkerTest {

    /**
     * Reflectively call SwingWorker's protected {@code process(List)} hook. ScanWorker is
     * {@code final}, so a test subclass isn't an option; reflection is the cleanest path
     * to deterministic coverage of the method body.
     */
    private static void invokeProcess(ScanWorker worker, List<Finding> chunks) throws Exception {
        java.lang.reflect.Method m = ScanWorker.class.getDeclaredMethod("process", List.class);
        m.setAccessible(true);
        m.invoke(worker, chunks);
    }

    @Test
    void doInBackgroundReturnsScanResult() throws ExecutionException, InterruptedException {
        JMeterTreeModel model = JmxTestHarness.emptyPlan();
        List<Finding> progressFindings = new ArrayList<>();
        ScanWorker w = new ScanWorker(model, Set.of(), null, "x.jmx",
                "5.6.3", false, progressFindings::add);

        w.execute();
        ScanResult r = w.get();

        assertThat(r).isNotNull();
        assertThat(r.outcome()).isEqualTo(ScanOutcome.COMPLETE);
        assertThat(r.jmxFileName()).isEqualTo("x.jmx");
        assertThat(r.jmeterVersion()).isEqualTo("5.6.3");
    }

    @Test
    void processForwardsToProgressCallback() throws Exception {
        // Cover process() deterministically by invoking it via reflection — the EDT-pump
        // path (publish() → invokeLater → process) is non-deterministic in surefire's
        // headless-but-EDT-alive runtime; reflection sidesteps the timing dance.
        JMeterTreeModel model = JmxTestHarness.emptyPlan();
        AtomicInteger received = new AtomicInteger();
        ScanWorker w = new ScanWorker(model, Set.of(), null, "x.jmx", "5.6.3", false,
                f -> received.incrementAndGet());

        Finding f = new Finding("X",
                io.github.sagaraggarwal86.jmeter.jmxauditor.model.Category.CORRECTNESS,
                io.github.sagaraggarwal86.jmeter.jmxauditor.model.Severity.ERROR,
                "t", "d", "s",
                new io.github.sagaraggarwal86.jmeter.jmxauditor.model.NodePath(List.of("Test Plan")), 1);

        invokeProcess(w, List.of(f, f));
        assertThat(received.get()).isEqualTo(2);
    }

    @Test
    void processIsNoOpWhenProgressIsNull() throws Exception {
        // Same trigger but progress=null — exercises the early-return branch of process().
        JMeterTreeModel model = JmxTestHarness.emptyPlan();
        ScanWorker w = new ScanWorker(model, Set.of(), null, "x.jmx", "5.6.3", false, null);
        Finding f = new Finding("X",
                io.github.sagaraggarwal86.jmeter.jmxauditor.model.Category.CORRECTNESS,
                io.github.sagaraggarwal86.jmeter.jmxauditor.model.Severity.ERROR,
                "t", "d", "s",
                new io.github.sagaraggarwal86.jmeter.jmxauditor.model.NodePath(List.of("Test Plan")), 1);

        // Just must not throw NPE.
        invokeProcess(w, List.of(f));
    }
}
