package io.github.sagaraggarwal86.jmeter.jauditor.rules.security;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.testelement.TestElement;

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
            Argument a = args.getArgument(i);
            String name = a.getName();
            String val = a.getValue();
            if (name == null || val == null) continue;
            Matcher m = KEY.matcher(name.trim());
            if (!m.matches()) continue;
            if (hasJMeterVar(val)) continue;
            if (val.isBlank()) continue;
            out.add(make(ctx.pathFor(node),
                    "Plaintext credential in request body",
                    "The HTTP request sends the field '" + name + "' with a hard-coded value. That value lives directly inside the .jmx file, so anyone who opens the test plan or checks it into version control can read the real credential. Passwords and tokens written into .jmx files are a common source of accidental leaks, especially when the file ends up in a CI log or a screenshot. Value redacted to " + io.github.sagaraggarwal86.jmeter.jauditor.util.JAuditorLog.redact(val) + " — JAuditor never prints credential contents.",
                    "Move the actual value out of the .jmx. Typical options: load it from a CSV file at runtime (useful when each thread needs a different credential), read it from an environment variable using ${__env(NAME)} inside a User Defined Variables block, or fetch it from a secrets manager via a JSR223 PreProcessor. Then replace the hard-coded value here with a JMeter variable reference like ${PASSWORD}, so the test plan can be shared and reviewed without exposing the real secret."));
        }
        return out;
    }
}
