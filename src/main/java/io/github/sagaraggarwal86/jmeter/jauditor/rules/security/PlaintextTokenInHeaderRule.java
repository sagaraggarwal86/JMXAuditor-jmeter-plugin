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
            if (stripped.toLowerCase().startsWith("bearer ")) stripped = stripped.substring(7).trim();
            if (stripped.isEmpty()) continue;
            out.add(make(ctx.pathFor(node),
                    "Plaintext token in Authorization header",
                    "Header Manager contains a literal Authorization value (redacted: " + io.github.sagaraggarwal86.jmeter.jauditor.util.JAuditorLog.redact(val) + ").",
                    "Move the token to a ${variable} populated from environment or CSV."));
        }
        return out;
    }
}
