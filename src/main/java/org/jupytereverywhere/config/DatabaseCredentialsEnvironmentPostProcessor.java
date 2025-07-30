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

        Map<String, Object> propertyMap = new HashMap<>();

        if (dbCredentialsJson != null && !dbCredentialsJson.isEmpty()) {
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
        } else if ((dbUsername == null || dbUsername.isEmpty()) || (dbPassword == null || dbPassword.isEmpty())) {
            throw new IllegalStateException("Either DB_CREDENTIALS or both DB_USERNAME and DB_PASSWORD environment variables must be set.");
        }

        if (!propertyMap.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dbCredentialsOverride", propertyMap));
        }
    }
}
