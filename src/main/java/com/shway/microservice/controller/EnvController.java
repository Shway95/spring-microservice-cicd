package com.shway.microservice.controller;

import com.shway.microservice.config.AppConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/env")
public class EnvController {

    private final AppConfig appConfig;

    public EnvController(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @GetMapping
    public Map<String, String> getEnvironmentInfo() {
        return Map.of(
            "profile", appConfig.getActiveProfile(),
            "apiBaseUrl", appConfig.getApiBaseUrl(),
            "logLevel", appConfig.getLogLevel()
            // Note: never expose secrets like API keys in an endpoint!
        );
    }
}
