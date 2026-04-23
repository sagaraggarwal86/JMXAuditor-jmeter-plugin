package io.github.sagaraggarwal86.jmeter.jauditor.export.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.github.sagaraggarwal86.jmeter.jauditor.model.ScanResult;

public final class JAuditorObjectMapper {

    private JAuditorObjectMapper() {
    }

    public static ObjectMapper create() {
        return JsonMapper.builder()
                .addModule(new ParameterNamesModule())
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addMixIn(ScanResult.class, ScanResultMixin.class)
                .build();
    }

    abstract static class ScanResultMixin {
        @JsonIgnore
        abstract Object navigation();
    }
}
