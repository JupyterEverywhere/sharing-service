package org.jupytereverywhere.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.log4j.Log4j2;


/**
 * Health check controller for monitoring the API status.
 */
@Log4j2
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        log.info("Health check endpoint accessed");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Sharing Service API is running");
        response.put("timestamp", Instant.now().toString());
        response.put("service", "sharing-service");
        response.put("version", "0.1.1");
        
        return ResponseEntity.ok(response);
    }
}
