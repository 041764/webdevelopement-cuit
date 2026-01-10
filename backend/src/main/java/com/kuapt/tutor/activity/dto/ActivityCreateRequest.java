package com.kuapt.tutor.activity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record ActivityCreateRequest(
    @Positive long classId,
    @NotBlank String term,
    @NotBlank String title,
    String description,
    @Min(1) Integer capacity,
    boolean requiresReview,
    Instant startsAt,
    Instant endsAt) {}
