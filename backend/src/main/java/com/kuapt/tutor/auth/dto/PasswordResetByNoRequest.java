package com.kuapt.tutor.auth.dto;

import com.kuapt.tutor.model.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasswordResetByNoRequest(
    @NotNull UserType userType,
    @NotBlank String userNo,
    @NotBlank String clientSalt,
    @NotBlank String clientHash) {}
