package com.kuapt.tutor.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password")
public record PasswordProperties(String pepper) {}
