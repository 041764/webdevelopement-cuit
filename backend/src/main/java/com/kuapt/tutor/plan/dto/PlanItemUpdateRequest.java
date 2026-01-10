package com.kuapt.tutor.plan.dto;

import com.kuapt.tutor.model.PlanItemStatus;

public record PlanItemUpdateRequest(String title, PlanItemStatus status, Integer sortOrder, String dueDate) {}

