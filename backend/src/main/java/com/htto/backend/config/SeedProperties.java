package com.htto.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        boolean enabled,
        String adminUsername,
        String adminEmail,
        String adminPassword,
        String teacherUsername,
        String teacherEmail,
        String teacherPassword,
        String studentUsername,
        String studentEmail,
        String studentPassword,
        String student2Username,
        String student2Email,
        String student2Password
) {
}
