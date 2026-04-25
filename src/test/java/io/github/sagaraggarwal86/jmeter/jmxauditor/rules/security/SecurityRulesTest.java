package io.github.sagaraggarwal86.jmeter.jmxauditor.rules.security;

import io.github.sagaraggarwal86.jmeter.jmxauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness;
import io.github.sagaraggarwal86.jmeter.jmxauditor.support.JmxTestHarness.NodeWithModel;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityRulesTest {

    // ─── PLAINTEXT_PASSWORD_IN_BODY ─────────────────────────

    @Test
    void plaintextPassword_firesOnLiteralPasswordArg() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        Arguments args = new Arguments();
        args.addArgument("password", "hunter2");
        http.setArguments(args);
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new PlaintextPasswordInBodyRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("PLAINTEXT_PASSWORD_IN_BODY");
        // Redaction invariant 9: the actual password value never reaches the description.
        assertThat(findings.get(0).description()).contains("****").doesNotContain("hunter2");
    }

    @Test
    void plaintextPassword_firesOnTokenLikeArgs() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        Arguments args = new Arguments();
        args.addArgument("token", "abcd-1234");
        args.addArgument("api_key", "AKIA...");
        args.addArgument("secret", "shh");
        http.setArguments(args);
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new PlaintextPasswordInBodyRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(3);
    }

    @Test
    void plaintextPassword_doesNotFireWhenValueIsJMeterVar() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        Arguments args = new Arguments();
        args.addArgument("password", "${PASSWORD}");
        http.setArguments(args);
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new PlaintextPasswordInBodyRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void plaintextPassword_doesNotFireForNonCredentialFieldNames() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        Arguments args = new Arguments();
        args.addArgument("user_name", "alice");
        args.addArgument("email", "alice@example.com");
        http.setArguments(args);
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new PlaintextPasswordInBodyRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void plaintextPassword_doesNotFireForBlankValue() {
        HTTPSamplerProxy http = new HTTPSamplerProxy();
        Arguments args = new Arguments();
        args.addArgument("password", "");
        http.setArguments(args);
        NodeWithModel n = JmxTestHarness.singleElement(http);
        List<Finding> findings = new PlaintextPasswordInBodyRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── PLAINTEXT_TOKEN_IN_HEADER ──────────────────────────

    @Test
    void plaintextToken_firesOnLiteralBearer() {
        HeaderManager hm = new HeaderManager();
        hm.add(new Header("Authorization", "Bearer eyJhbGc..."));
        NodeWithModel n = JmxTestHarness.singleElement(hm);
        List<Finding> findings = new PlaintextTokenInHeaderRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("PLAINTEXT_TOKEN_IN_HEADER");
        assertThat(findings.get(0).description()).contains("****").doesNotContain("eyJhbGc");
    }

    @Test
    void plaintextToken_firesOnRawAuthorizationValue() {
        HeaderManager hm = new HeaderManager();
        hm.add(new Header("authorization", "raw-token-here"));
        NodeWithModel n = JmxTestHarness.singleElement(hm);
        List<Finding> findings = new PlaintextTokenInHeaderRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
    }

    @Test
    void plaintextToken_doesNotFireWhenValueIsJMeterVar() {
        HeaderManager hm = new HeaderManager();
        hm.add(new Header("Authorization", "Bearer ${TOKEN}"));
        NodeWithModel n = JmxTestHarness.singleElement(hm);
        List<Finding> findings = new PlaintextTokenInHeaderRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void plaintextToken_doesNotFireForNonAuthHeaders() {
        HeaderManager hm = new HeaderManager();
        hm.add(new Header("Content-Type", "application/json"));
        hm.add(new Header("X-Request-Id", "abc"));
        NodeWithModel n = JmxTestHarness.singleElement(hm);
        List<Finding> findings = new PlaintextTokenInHeaderRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void plaintextToken_doesNotFireForEmptyAuthValue() {
        // Empty value: stripped.isEmpty() short-circuits before any finding is created.
        // Note: "Bearer " alone trims to "Bearer" (no trailing space), so the prefix-strip
        // path doesn't run — the rule fires for "Bearer " alone, which is intentional;
        // the "blank value" escape only applies to genuinely empty values.
        HeaderManager hm = new HeaderManager();
        hm.add(new Header("Authorization", ""));
        NodeWithModel n = JmxTestHarness.singleElement(hm);
        List<Finding> findings = new PlaintextTokenInHeaderRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    // ─── CREDENTIALS_IN_UDV ─────────────────────────────────

    @Test
    void credentialsInUdv_firesOnLiteralPasswordVar() {
        Arguments udv = new Arguments();
        udv.setName("UDV");
        udv.addArgument(new Argument("admin_password", "letmein", "="));
        NodeWithModel n = JmxTestHarness.singleElement(udv);
        List<Finding> findings = new CredentialsInUdvRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleId()).isEqualTo("CREDENTIALS_IN_UDV");
        assertThat(findings.get(0).description()).contains("****").doesNotContain("letmein");
    }

    @Test
    void credentialsInUdv_doesNotFireWhenValueIsJMeterVar() {
        Arguments udv = new Arguments();
        udv.addArgument(new Argument("api_token", "${API_TOKEN}", "="));
        NodeWithModel n = JmxTestHarness.singleElement(udv);
        List<Finding> findings = new CredentialsInUdvRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }

    @Test
    void credentialsInUdv_doesNotFireForNonCredentialNames() {
        Arguments udv = new Arguments();
        udv.addArgument(new Argument("base_url", "https://example.com", "="));
        NodeWithModel n = JmxTestHarness.singleElement(udv);
        List<Finding> findings = new CredentialsInUdvRule().check(n.node(), JmxTestHarness.newContext(n.model()));
        assertThat(findings).isEmpty();
    }
}
