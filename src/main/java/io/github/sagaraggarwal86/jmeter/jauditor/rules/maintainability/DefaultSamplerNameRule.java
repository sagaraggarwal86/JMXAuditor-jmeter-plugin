package io.github.sagaraggarwal86.jmeter.jauditor.rules.maintainability;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;

import java.util.List;
import java.util.Set;

public final class DefaultSamplerNameRule extends AbstractRule {

    private static final Set<String> DEFAULTS = Set.of(
            "HTTP Request", "Debug Sampler", "JSR223 Sampler", "JDBC Request",
            "SOAP/XML-RPC Request", "FTP Request", "TCP Sampler", "JMS Publisher",
            "JMS Subscriber", "Java Request", "BeanShell Sampler"
    );

    @Override
    public String id() {
        return "DEFAULT_SAMPLER_NAME";
    }

    @Override
    public Category category() {
        return Category.MAINTAINABILITY;
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public String description() {
        return "Sampler keeps JMeter's default name (hard to read in reports).";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(Sampler.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        String name = node.getName();
        if (name == null) return List.of();
        if (!DEFAULTS.contains(name.trim())) return List.of();
        return List.of(make(ctx.pathFor(node),
                "Sampler uses default name",
                "Sampler '" + name + "' has not been renamed.",
                "Rename it to describe the business action (e.g., 'POST /checkout')."));
    }
}
