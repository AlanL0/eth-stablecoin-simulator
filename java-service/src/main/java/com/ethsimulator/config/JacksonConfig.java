package com.ethsimulator.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer financialDecimalSerialization() {
        return builder -> builder.addModule(new SimpleModule()
                .addSerializer(BigDecimal.class, new PlainBigDecimalSerializer()));
    }

    private static final class PlainBigDecimalSerializer extends StdSerializer<BigDecimal> {

        private PlainBigDecimalSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctxt) {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeString(value.toPlainString());
        }
    }
}