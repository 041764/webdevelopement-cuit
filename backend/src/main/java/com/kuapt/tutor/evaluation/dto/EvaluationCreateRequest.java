package com.kuapt.tutor.evaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EvaluationCreateRequest(
    @NotNull Long evaluateeUserId,
    @NotBlank String term,
    @NotNull @Min(0) Integer scoreTotal,
    String comment,
    @Valid List<EvaluationDetailItem> details) {}
