package org.jupytereverywhere.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.log4j.Log4j2;

import org.jupytereverywhere.service.ApplicationInfoService;


/**
 * Health check controller for monitoring the API status.
 */
@Log4j2
@RestController
@RequestMapping("/health")
public class HealthController {

    private final ApplicationInfoService applicationInfoService;

    public HealthController(ApplicationInfoService applicationInfoService) {
        this.applicationInfoService = applicationInfoService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        log.info("Health check endpoint accessed");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Sharing Service API is running");
        response.put("timestamp", Instant.now().toString());
        response.put("service", applicationInfoService.getName());
        response.put("version", applicationInfoService.getVersion());

        return ResponseEntity.ok(response);
    }
}
