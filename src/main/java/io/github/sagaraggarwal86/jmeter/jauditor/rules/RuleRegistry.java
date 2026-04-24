package io.github.sagaraggarwal86.jmeter.jauditor.rules;

import io.github.sagaraggarwal86.jmeter.jauditor.rules.correctness.AssertionScopeMismatchRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.correctness.ExtractorNoDefaultRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.correctness.ExtractorNoReferenceNameRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.correctness.ThreadGroupZeroDurationRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.maintainability.*;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.observability.HttpSamplerNoAssertionRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.observability.Jsr223NoCacheKeyRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.observability.TransactionParentSampleRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.observability.UnnamedTransactionControllerRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.realism.MissingCookieManagerRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.realism.MissingRampUpRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.realism.NoThinkTimesRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.scalability.*;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.security.CredentialsInUdvRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.security.PlaintextPasswordInBodyRule;
import io.github.sagaraggarwal86.jmeter.jauditor.rules.security.PlaintextTokenInHeaderRule;

import java.util.List;

/**
 * Immutable rule catalogue in PRD §7 order. Whole-tree rules appear first within each
 * category so their memo keys are populated before dependent per-node rules run.
 */
public final class RuleRegistry {

    private static final List<Rule> RULES = List.of(
            // Correctness
            new ExtractorNoDefaultRule(),
            new ThreadGroupZeroDurationRule(),
            new AssertionScopeMismatchRule(),
            new ExtractorNoReferenceNameRule(),
            // Security
            new PlaintextPasswordInBodyRule(),
            new PlaintextTokenInHeaderRule(),
            new CredentialsInUdvRule(),
            // Scalability
            new GuiListenerInLoadPathRule(),
            new BeanshellUsageRule(),
            new SaveResponseDataEnabledRule(),
            new RetrieveEmbeddedResourcesRule(),
            new ThreadCountExcessiveRule(),
            // Realism — whole-tree first
            new MissingCookieManagerRule(),
            new NoThinkTimesRule(),
            new MissingRampUpRule(),
            // Maintainability — whole-tree first
            new JtlExcessiveSaveFieldsRule(),
            new MissingTransactionControllerRule(),
            new HardcodedHostRule(),
            new DefaultSamplerNameRule(),
            new DisabledElementInTreeRule(),
            new CsvAbsolutePathRule(),
            // Observability
            new HttpSamplerNoAssertionRule(),
            new UnnamedTransactionControllerRule(),
            new TransactionParentSampleRule(),
            new Jsr223NoCacheKeyRule()
    );

    private RuleRegistry() {
    }

    public static List<Rule> all() {
        return RULES;
    }

    public static int count() {
        return RULES.size();
    }
}
