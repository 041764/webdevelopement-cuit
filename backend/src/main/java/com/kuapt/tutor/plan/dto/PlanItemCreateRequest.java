package com.kuapt.tutor.plan.dto;

import jakarta.validation.constraints.NotBlank;

public record PlanItemCreateRequest(@NotBlank String title, String dueDate) {}

