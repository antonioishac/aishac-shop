package com.aishac.sales.config;

import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.JsonWriter.ValueProcessor;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Aplica {@link SensitiveDataMasker} a todo valor texto do log JSON (mensagem, MDC, exceção). Registrado no
 * config-repo por {@code logging.structured.json.customizer}.
 */
public class SensitiveDataMaskingCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

    @Override
    public void customize(Members<Object> members) {
        members.applyingValueProcessor(ValueProcessor.of(String.class, SensitiveDataMasker::mask));
    }
}
