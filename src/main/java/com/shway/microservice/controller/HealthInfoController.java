package com.shway.microservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/info")
public class HealthInfoController {

    @Value("${spring.application.name:unknown}")
    private String appName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private final LocalDateTime startTime = LocalDateTime.now();

    @GetMapping
    public Map<String, Object> getInfo() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);

        return Map.of(
            "application", appName,
            "profile", activeProfile,
            "uptime", String.format("%d hours, %d minutes", uptime.toHours(), uptime.toMinutesPart()),
            "startedAt", startTime.toString(),
            "javaVersion", System.getProperty("java.version"),
            "os", System.getProperty("os.name") + " " + System.getProperty("os.arch")
        );
    }
}
