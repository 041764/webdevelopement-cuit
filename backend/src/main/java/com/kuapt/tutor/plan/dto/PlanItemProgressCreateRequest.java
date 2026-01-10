package com.kuapt.tutor.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlanItemProgressCreateRequest(@NotNull @Min(0) @Max(100) Integer percent, String note) {}

