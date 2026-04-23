package io.github.sagaraggarwal86.jmeter.jauditor.rules.security;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import io.github.sagaraggarwal86.jmeter.jauditor.util.JAuditorLog;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class CredentialsInUdvRule extends AbstractRule {

    private static final Pattern KEY = Pattern.compile("(?i).*(password|secret|token|apikey|api_key).*");

    @Override
    public String id() {
        return "CREDENTIALS_IN_UDV";
    }

    @Override
    public Category category() {
        return Category.SECURITY;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String description() {
        return "User Defined Variable with credential-like name containing a literal value.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(Arguments.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        TestElement te = node.getTestElement();
        if (!"Arguments".equals(te.getClass().getSimpleName()) && !(te instanceof Arguments)) return List.of();
        Arguments a = (Arguments) te;
        List<Finding> out = new ArrayList<>();
        for (int i = 0; i < a.getArgumentCount(); i++) {
            Argument arg = a.getArgument(i);
            String name = arg.getName();
            String val = arg.getValue();
            if (name == null || val == null) continue;
            if (!KEY.matcher(name).matches()) continue;
            if (val.isBlank() || hasJMeterVar(val)) continue;
            out.add(make(ctx.pathFor(node),
                    "Credential literal in User Defined Variables",
                    "Variable '" + name + "' holds a literal value (redacted: " + JAuditorLog.redact(val) + ").",
                    "Load the value from environment, CSV, or a vault at runtime — do not store secrets in .jmx."));
        }
        return out;
    }
}
