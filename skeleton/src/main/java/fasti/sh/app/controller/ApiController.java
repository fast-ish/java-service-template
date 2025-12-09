package fasti.sh.app.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.annotation.Timed;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    @Value("${spring.application.name:app}")
    private String appName;

    @Value("${APP_VERSION:0.1.0}")
    private String appVersion;

    @Value("${ENVIRONMENT:development}")
    private String environment;

    @GetMapping("/status")
    @Timed(value = "api.status", description = "Time taken to return status")
    public Map<String, Object> status() {
        return Map.of(
            "status", "ok",
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/info")
    @Timed(value = "api.info", description = "Time taken to return info")
    public Map<String, Object> info() {
        return Map.of(
            "name", appName,
            "version", appVersion,
            "environment", environment,
            "java", System.getProperty("java.version"),
            "timestamp", Instant.now().toString()
        );
    }
}
