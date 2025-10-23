package org.jupytereverywhere.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;

@Log4j2
@Component
public class CorsStartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${cors.enabled:false}")
    private boolean corsEnabled;

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:}")
    private String allowedMethods;

  @Value("${cors.allowed-headers:}")
  private String allowedHeaders;

  @Value("${cors.exposed-headers:}")
  private String exposedHeaders;

  @Value("${cors.allow-credentials:true}")
  private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        logCorsConfiguration();
    }

    private void logCorsConfiguration() {
        StringMapMessage corsConfigLog = new StringMapMessage()
            .with("action", "logCorsConfiguration")
            .with("corsEnabled", String.valueOf(corsEnabled));

        if (corsEnabled) {
            corsConfigLog
                .with("allowedOrigins", formatConfigValue(allowedOrigins, "*"))
                .with("allowedMethods", formatConfigValue(allowedMethods, "*"))
                .with("allowedHeaders", formatConfigValue(allowedHeaders, "*"))
                .with("exposedHeaders", formatConfigValue(exposedHeaders, "Default headers"))
                .with("allowCredentials", String.valueOf(allowCredentials))
                .with("maxAge", String.valueOf(maxAge) + " seconds");
        } else {
            corsConfigLog.with("note", "Using Spring Security default CORS handling");
        }

        log.info(corsConfigLog);

        // Also log a human-readable summary
        if (corsEnabled) {
            log.info("🌐 CORS Configuration Active:");
            log.info("   └─ Origins: {}", formatConfigValue(allowedOrigins, "All origins (*)"));
            log.info("   └─ Methods: {}", formatConfigValue(allowedMethods, "All methods (*)"));
            log.info("   └─ Headers: {}", formatConfigValue(allowedHeaders, "All headers (*)"));
            log.info("   └─ Exposed Headers: {}", formatConfigValue(exposedHeaders, "Default headers (Authorization, Content-Type, etc.)"));
            log.info("   └─ Credentials: {}", allowCredentials ? "Allowed" : "Not allowed");
            log.info("   └─ Max Age: {} seconds", maxAge);
        } else {
            log.info("🌐 CORS Configuration: Using Spring Security defaults (custom CORS disabled)");
        }
    }

    private String formatConfigValue(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        // If it's a comma-separated list, format it nicely
        if (value.contains(",")) {
            return Arrays.toString(value.split(",")).replaceAll("[\\[\\]]", "");
        }

        return value;
    }
}
