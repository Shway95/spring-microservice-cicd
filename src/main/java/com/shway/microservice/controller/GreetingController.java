package com.shway.microservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/greet")
public class GreetingController {

    @GetMapping
    public Map<String, String> greet() {
        return Map.of(
            "message", "Hello from Spring Microservice!",
            "timestamp", LocalDateTime.now().toString(),
            "version", "1.0.0"
        );
    }

    @GetMapping("/{name}")
    public Map<String, String> greetByName(@PathVariable String name) {
        return Map.of(
            "message", "Hello, " + name + "!",
            "timestamp", LocalDateTime.now().toString()
        );
    }
}
