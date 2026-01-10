package com.kuapt.tutor.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank String clientSalt, @NotBlank String clientHash) {}
