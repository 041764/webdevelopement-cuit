package com.kuapt.tutor.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String issuer, Duration accessTtl, Duration refreshTtl, String secret) {}
