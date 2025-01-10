package com.vini_energia.auth_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final MongoTemplate mongoTemplate;
    private final Instant startupTime;

    @Autowired
    public StatusController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.startupTime = Instant.now();
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now());
        status.put("version", "1.0.0");
        status.put("uptime", getUptime());

        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            status.put("database", "CONNECTED");
        } catch (Exception e) {
            status.put("database", "DISCONNECTED");
            status.put("error", e.getMessage());
        }

        return status;
    }

    private String getUptime() {
        Duration uptime = Duration.between(startupTime, Instant.now());
        return String.format("%d days, %d hours, %d minutes, %d seconds",
                uptime.toDaysPart(),
                uptime.toHoursPart(),
                uptime.toMinutesPart(),
                uptime.toSecondsPart());
    }
}

