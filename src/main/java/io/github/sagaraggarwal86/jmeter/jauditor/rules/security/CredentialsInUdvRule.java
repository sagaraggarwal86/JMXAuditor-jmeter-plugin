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
                    "The User Defined Variable '" + name + "' has a name that looks like a credential (password, token, secret, apikey) and holds a hard-coded value. Because User Defined Variables live inside the .jmx, this value travels with the test plan everywhere it goes — into git, into screenshots, into CI job logs. That's almost never what the author intends. Value redacted to " + JAuditorLog.redact(val) + " — JAuditor never prints credential contents.",
                    "Replace the literal value with something that resolves at runtime. Common options: ${__env(VAR_NAME)} to read from an environment variable, ${__P(prop.name)} to read from a JMeter property passed on the command line (jmeter -Jprop.name=value ...), or a CSV Data Set Config if every row needs its own credential. The variable name can stay exactly the same, so the rest of the test plan doesn't need to change — only the stored value moves out of the .jmx."));
        }
        return out;
    }
}
