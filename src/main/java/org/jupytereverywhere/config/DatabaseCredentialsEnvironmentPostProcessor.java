package org.jupytereverywhere.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DatabaseCredentialsEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbCredentialsJson = System.getenv("DB_CREDENTIALS");
        String dbUsername = System.getenv("DB_USERNAME");
        String dbPassword = System.getenv("DB_PASSWORD");

        // Fallback to system properties if env vars are not set (for test environments)
        if (dbUsername == null || dbUsername.isEmpty()) {
            dbUsername = System.getProperty("DB_USERNAME");
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            dbPassword = System.getProperty("DB_PASSWORD");
        }

        Map<String, Object> propertyMap = new HashMap<>();

        boolean hasCredentialsJson = dbCredentialsJson != null && !dbCredentialsJson.isEmpty();
        boolean hasUsername = dbUsername != null && !dbUsername.isEmpty();
        boolean hasPassword = dbPassword != null && !dbPassword.isEmpty();

        if (hasCredentialsJson && (hasUsername || hasPassword)) {
            throw new IllegalStateException("Only one of DB_CREDENTIALS or (DB_USERNAME and DB_PASSWORD) may be set, not both.");
        }

        if (hasCredentialsJson) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(dbCredentialsJson);
                String username = node.has("username") ? node.get("username").asText() : null;
                String password = node.has("password") ? node.get("password").asText() : null;
                if (username != null) {
                    propertyMap.put("DB_USERNAME", username);
                    propertyMap.put("spring.datasource.username", username);
                }
                if (password != null) {
                    propertyMap.put("DB_PASSWORD", password);
                    propertyMap.put("spring.datasource.password", password);
                }
            } catch (Exception e) {
                System.err.println("[DatabaseCredentialsEnvironmentPostProcessor] Failed to parse DB_CREDENTIALS: " + e.getMessage());
                throw new IllegalStateException("DB_CREDENTIALS is set but could not be parsed as valid JSON with 'username' and 'password' fields.", e);
            }
        } else if (!hasUsername || !hasPassword) {
            throw new IllegalStateException("Either DB_CREDENTIALS or both DB_USERNAME and DB_PASSWORD environment variables must be set.");
        } else {
            propertyMap.put("DB_USERNAME", dbUsername);
            propertyMap.put("spring.datasource.username", dbUsername);
            propertyMap.put("DB_PASSWORD", dbPassword);
            propertyMap.put("spring.datasource.password", dbPassword);
        }

        if (!propertyMap.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dbCredentialsOverride", propertyMap));
        }
    }
}
