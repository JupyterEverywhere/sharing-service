package org.jupytereverywhere.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson configuration for stricter JSON deserialization.
 *
 * <p>Configured to fail on: - Null values for primitive types (int, boolean, etc.) - Trailing
 * tokens after valid JSON - Null values passed to constructor parameters
 *
 * <p>Note: This does not prevent null values for missing object fields during deserialization.
 * Use @NotNull with Bean Validation to enforce required fields.
 */
@Configuration
public class JacksonConfig {

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Don't serialize null values - prevents schema validation issues with optional fields
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // Fail if required fields are missing (instead of setting to null)
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

    // Fail if JSON contains unknown properties (helps catch client errors)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Fail if JSON has trailing tokens after valid JSON
    mapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);

    // Fail if JSON reads into a null value for creator (constructor) properties
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);

    return mapper;
  }
}
