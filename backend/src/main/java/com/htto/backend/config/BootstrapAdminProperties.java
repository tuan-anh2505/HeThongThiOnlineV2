package com.htto.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(
        boolean enabled,
        String username,
        String email,
        String password,
        String fullName,
        String adminCode
) {
}
