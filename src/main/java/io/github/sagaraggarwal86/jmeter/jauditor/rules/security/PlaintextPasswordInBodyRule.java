package io.github.sagaraggarwal86.jmeter.jauditor.rules.security;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaintextPasswordInBodyRule extends AbstractRule {

    private static final Pattern KEY = Pattern.compile("(?i)^(password|passwd|pwd|secret|token|apikey|api_key)$");

    @Override
    public String id() {
        return "PLAINTEXT_PASSWORD_IN_BODY";
    }

    @Override
    public Category category() {
        return Category.SECURITY;
    }

    @Override
    public Severity severity() {
        return Severity.ERROR;
    }

    @Override
    public String description() {
        return "HTTP sampler body contains a literal credential value.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(HTTPSamplerBase.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        HTTPSamplerBase http = (HTTPSamplerBase) node.getTestElement();
        List<Finding> out = new ArrayList<>();
        Arguments args = http.getArguments();
        if (args == null) return out;
        for (int i = 0; i < args.getArgumentCount(); i++) {
            HTTPArgument a = (HTTPArgument) args.getArgument(i);
            String name = a.getName();
            String val = a.getValue();
            if (name == null || val == null) continue;
            Matcher m = KEY.matcher(name.trim());
            if (!m.matches()) continue;
            if (hasJMeterVar(val)) continue;
            if (val.isBlank()) continue;
            out.add(make(ctx.pathFor(node),
                    "Plaintext credential in request body",
                    "Argument '" + name + "' contains a literal value (redacted: " + io.github.sagaraggarwal86.jmeter.jauditor.util.JAuditorLog.redact(val) + ").",
                    "Replace the literal value with a ${variable} sourced from a CSV or User Defined Variables — never commit credentials to .jmx."));
        }
        // fallback: raw body data
        JMeterProperty post = http.getProperty("HTTPsampler.postBodyRaw");
        if (post instanceof CollectionProperty) {
            // ignore — raw body handled via Arguments above
        }
        return out;
    }
}
