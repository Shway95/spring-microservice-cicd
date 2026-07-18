package com.shway.microservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${app.api-base-url}")
    private String apiBaseUrl;

    @Value("${app.api-key}")
    private String apiKey;

    @Value("${app.log-level}")
    private String logLevel;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public String getApiBaseUrl() { return apiBaseUrl; }
    public String getApiKey() { return apiKey; }
    public String getLogLevel() { return logLevel; }
    public String getActiveProfile() { return activeProfile; }
}
