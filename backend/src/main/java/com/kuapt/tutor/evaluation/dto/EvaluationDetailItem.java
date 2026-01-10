package com.kuapt.tutor.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluationDetailItem(@NotBlank String itemKey, @NotNull Integer score, String comment) {}
