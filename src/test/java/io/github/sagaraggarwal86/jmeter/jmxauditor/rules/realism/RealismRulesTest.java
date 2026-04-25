package io.github.sagaraggarwal86.jmeter.jmxauditor.rules.realism;

import io.github.sagaraggarwal86.jmeter.jmxauditor.engine.RuleEngine;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanResult;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness.NodeWithModel;
import io.github.sagaraggarwal86.jmeter.jmxauditor.util.Clock;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.timers.ConstantTimer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class RealismRulesTest {

    // ─── MISSING_COOKIE_MANAGER (whole-tree) ────────────────

    private static ScanResult scanWith(JmxTestHarness.TreeSpec spec) {
        var nm = JmxTestHarness.planWithChildren(spec);
        return RuleEngine.scan(nm.model(), Set.of(), null, "x.jmx", "5.6.3", false, Clock.system(), null);
    }

    @Test
    void missingCookieManager_firesWhenHttpButNoCookieManager() {
        // Test plan with one HTTP sampler under a Thread Group, no CookieManager anywhere.
        ScanResult r = scanWith(b -> {
            ThreadGroup tg = new ThreadGroup();
            tg.setName("TG");
            var tgNode = b.add(tg, 0);
            HTTPSamplerProxy http = new HTTPSamplerProxy();
            http.setName("HTTP");
            b.under(tgNode).add(http);
        });
        assertThat(r.findings()).extracting(Finding::ruleId).contains("MISSING_COOKIE_MANAGER");
    }

    @Test
    void missingCookieManager_doesNotFireWhenCookieManagerPresent() {
        ScanResult r = scanWith(b -> {
            ThreadGroup tg = new ThreadGroup();
            var tgNode = b.add(tg, 0);
            b.under(tgNode).add(new HTTPSamplerProxy());
            b.under(tgNode).add(new CookieManager());
        });
        assertThat(r.findings()).extracting(Finding::ruleId).doesNotContain("MISSING_COOKIE_MANAGER");
    }

    // ─── NO_THINK_TIMES ─────────────────────────────────────

    @Test
    void missingCookieManager_doesNotFireWhenNoHttpSamplers() {
        // No HTTP samplers anywhere — rule short-circuits via the anyHttpSampler memo.
        ScanResult r = scanWith(b -> {
            b.add(new ThreadGroup(), 0);
        });
        assertThat(r.findings()).extracting(Finding::ruleId).doesNotContain("MISSING_COOKIE_MANAGER");
    }

    @Test
    void noThinkTimes_firesWhenSamplerWithoutTimer() {
        ScanResult r = scanWith(b -> {
            ThreadGroup tg = new ThreadGroup();
            var tgNode = b.add(tg, 0);
            b.under(tgNode).add(new HTTPSamplerProxy());
        });
        assertThat(r.findings()).extracting(Finding::ruleId).contains("NO_THINK_TIMES");
    }

    @Test
    void noThinkTimes_doesNotFireWhenTimerPresent() {
        ScanResult r = scanWith(b -> {
            ThreadGroup tg = new ThreadGroup();
            var tgNode = b.add(tg, 0);
            b.under(tgNode).add(new HTTPSamplerProxy());
            b.under(tgNode).add(new ConstantTimer());
        });
        assertThat(r.findings()).extracting(Finding::ruleId).doesNotContain("NO_THINK_TIMES");
    }

    // ─── MISSING_RAMP_UP ────────────────────────────────────

    @Test
    void noThinkTimes_doesNotFireWhenNoSamplers() {
        ScanResult r = scanWith(b -> {
            b.add(new ThreadGroup(), 0);
        });
        assertThat(r.findings()).extracting(Finding::ruleId).doesNotContain("NO_THINK_TIMES");
    }

    @Test
    void missingRampUp_firesWhenManyThreadsAndZeroRamp() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.num_threads", "50");
        tg.setProperty("ThreadGroup.ramp_time", "0");
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new MissingRampUpRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("MISSING_RAMP_UP");
    }

    @Test
    void missingRampUp_doesNotFireWith10OrFewerThreads() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.num_threads", "10");
        tg.setProperty("ThreadGroup.ramp_time", "0");
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new MissingRampUpRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void missingRampUp_doesNotFireWithPositiveRamp() {
        ThreadGroup tg = new ThreadGroup();
        tg.setProperty("ThreadGroup.num_threads", "100");
        tg.setProperty("ThreadGroup.ramp_time", "30");
        NodeWithModel n = JmxTestHarness.singleElement(tg);
        List<Finding> findings = new MissingRampUpRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }
}
