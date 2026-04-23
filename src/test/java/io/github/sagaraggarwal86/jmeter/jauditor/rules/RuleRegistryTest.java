package io.github.sagaraggarwal86.jmeter.jauditor.rules;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RuleRegistryTest {

    @Test
    void catalogueHas25Rules() {
        assertThat(RuleRegistry.count()).isEqualTo(25);
    }

    @Test
    void ruleIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (Rule r : RuleRegistry.all()) {
            assertThat(ids.add(r.id())).as("duplicate id " + r.id()).isTrue();
        }
    }

    @Test
    void ruleIdsAreUpperSnakeCase() {
        for (Rule r : RuleRegistry.all()) {
            assertThat(r.id()).matches("[A-Z][A-Z0-9_]*");
        }
    }

    @Test
    void categoryCounts() {
        int correctness = 0, security = 0, scalability = 0, realism = 0, maintainability = 0, observability = 0;
        for (Rule r : RuleRegistry.all()) {
            switch (r.category()) {
                case CORRECTNESS -> correctness++;
                case SECURITY -> security++;
                case SCALABILITY -> scalability++;
                case REALISM -> realism++;
                case MAINTAINABILITY -> maintainability++;
                case OBSERVABILITY -> observability++;
            }
        }
        assertThat(correctness).isEqualTo(4);
        assertThat(security).isEqualTo(3);
        assertThat(scalability).isEqualTo(5);
        assertThat(realism).isEqualTo(3);
        assertThat(maintainability).isEqualTo(6);
        assertThat(observability).isEqualTo(4);
    }

    @Test
    void everyRuleHasDescription() {
        for (Rule r : RuleRegistry.all()) {
            assertThat(r.description()).isNotBlank();
            assertThat(r.category()).isNotNull();
            assertThat(r.severity()).isNotNull();
        }
    }

    @Test
    void categoryEnumOrderMatchesPrd() {
        assertThat(Category.values()).containsExactly(
                Category.CORRECTNESS, Category.SECURITY, Category.SCALABILITY,
                Category.REALISM, Category.MAINTAINABILITY, Category.OBSERVABILITY);
    }
}
