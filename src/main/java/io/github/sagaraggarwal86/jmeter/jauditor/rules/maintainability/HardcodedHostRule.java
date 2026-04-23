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
        if (host == null || host.isBlank() || hasJMeterVar(host)) return List.of();
        if (!HOSTISH.matcher(host).matches()) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Hard-coded hostname",
                "Host '" + host + "' is a literal. Moving between environments requires editing the .jmx.",
                "Replace with ${HOST} backed by a User Defined Variable or property."));
    }
}
