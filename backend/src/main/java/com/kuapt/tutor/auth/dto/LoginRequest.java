package com.kuapt.tutor.auth.dto;

import com.kuapt.tutor.model.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
    @NotNull UserType userType,
    @NotBlank String id,
    @NotBlank String clientSalt,
    @NotBlank String clientHash,
    String deviceId) {}
