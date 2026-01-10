package com.kuapt.tutor.plan.dto;

import com.kuapt.tutor.model.PlanOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanCreateRequest(@NotNull PlanOwnerType ownerType, Long ownerClassId, @NotBlank String term, @NotBlank String title) {}

