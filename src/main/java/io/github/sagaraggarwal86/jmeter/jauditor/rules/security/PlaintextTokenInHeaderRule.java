package io.github.sagaraggarwal86.jmeter.jauditor.rules.security;

import io.github.sagaraggarwal86.jmeter.jauditor.engine.ScanContext;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.AbstractRule;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.testelement.TestElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PlaintextTokenInHeaderRule extends AbstractRule {
    @Override
    public String id() {
        return "PLAINTEXT_TOKEN_IN_HEADER";
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
        return "Header Manager with Authorization header containing a literal bearer token.";
    }

    @Override
    public Set<Class<? extends TestElement>> appliesTo() {
        return Set.of(HeaderManager.class);
    }

    @Override
    public List<Finding> check(JMeterTreeNode node, ScanContext ctx) {
        HeaderManager hm = (HeaderManager) node.getTestElement();
        List<Finding> out = new ArrayList<>();
        for (int i = 0; i < hm.getHeaders().size(); i++) {
            Header h = hm.get(i);
            if (h == null) continue;
            String name = h.getName();
            String val = h.getValue();
            if (name == null || val == null) continue;
            if (!"Authorization".equalsIgnoreCase(name.trim())) continue;
            if (hasJMeterVar(val)) continue;
            String stripped = val.trim();
            if (stripped.toLowerCase(Locale.ROOT).startsWith("bearer ")) stripped = stripped.substring(7).trim();
            if (stripped.isEmpty()) continue;
            out.add(make(ctx.pathFor(node),
                    "Plaintext token in Authorization header",
                    "This Header Manager sends an Authorization header with a bearer token written directly into the .jmx file. Anyone who opens the test plan — teammates, reviewers, anyone with access to the source repository — can read the real token. Tokens committed into test plans have a habit of staying valid long after the author meant to rotate them, and they often end up leaking into screenshots, CI logs, or chat messages. Value redacted to " + io.github.sagaraggarwal86.jmeter.jauditor.util.JAuditorLog.redact(val) + " — JAuditor never prints token contents.",
                    "Take the token out of the .jmx and feed it in at runtime. The usual pattern: read an environment variable via ${__env(AUTH_TOKEN)} inside a User Defined Variables block, or load a line from a CSV file with a CSV Data Set Config element. Then change the header value here from the literal token to a variable reference like 'Bearer ${AUTH_TOKEN}'. The test runs exactly the same way, but the test plan no longer carries the secret with it."));
        }
        return out;
    }
}
