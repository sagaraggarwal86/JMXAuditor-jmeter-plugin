package io.github.sagaraggarwal86.jmeter.jauditor.rules.maintainability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class HardcodedHostRule extends AbstractRule {

    private static final Pattern HOSTISH = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9.\\-]+(\\.[a-zA-Z]{2,})?$");

    @Override
    public String id() {
        return "HARDCODED_HOST";
    }

    @Override
    public Category category() {
        return Category.MAINTAINABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String description() {
        return "HTTP Sampler or HTTP Request Defaults with a literal hostname.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(HTTPSamplerBase.class, ConfigTestElement.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        String host = propString(te, "HTTPSampler.domain");
        if (host.isBlank() || hasJMeterVar(host)) return List.of();
        if (!HOSTISH.matcher(host).matches()) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Hard-coded hostname",
                "The Server Name field is set to '" + host + "' — a literal hostname written directly into the test plan. That ties this test to one specific environment. Anyone who wants to run the same test against dev, staging, or a branch deployment has to hand-edit the .jmx, which either means maintaining multiple copies of the file (drift hazard) or remembering to change it back before committing (leakage hazard).",
                "Replace the hard-coded hostname with a variable reference like ${HOST}. Define the variable either in a User Defined Variables block at the top of the test plan (easy to change per run from the GUI), or via a JMeter property passed on the command line (jmeter -JHOST=staging.example.com ...) so the same .jmx works across every environment without modification. For a multi-environment team, command-line properties are usually cleanest — the .jmx stays identical and the environment is picked at launch time."));
    }
}
