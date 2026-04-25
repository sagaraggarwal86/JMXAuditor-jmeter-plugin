package io.github.sagaraggarwal86.jmeter.jmxauditor.export.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.sagaraggarwal86.jmeter.jmxauditor.model.ScanResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JMXAuditorObjectMapperTest {

    @Test
    void scanResultMixinIsRegistered() {
        // Mixin presence is the contract — actual round-trip is covered in JsonReportWriterTest.
        // Verifying via findMixInClassFor avoids needing jackson-datatype-jsr310 for Instant.
        ObjectMapper m = JMXAuditorObjectMapper.create();
        assertThat(m.findMixInClassFor(ScanResult.class)).isNotNull();
    }

    @Test
    void nullValuesAreOmitted() throws Exception {
        ObjectMapper m = JMXAuditorObjectMapper.create();
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("a", "x");
        map.put("b", null);
        JsonNode root = m.readTree(m.writeValueAsString(map));
        assertThat(root.has("a")).isTrue();
        assertThat(root.has("b")).isFalse();
    }

    @Test
    void indentOutputEnabled() {
        ObjectMapper m = JMXAuditorObjectMapper.create();
        assertThat(m.isEnabled(SerializationFeature.INDENT_OUTPUT)).isTrue();
        // Dates as ISO strings, not numeric epoch — required by JSON schema 1.0.
        assertThat(m.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }
}
