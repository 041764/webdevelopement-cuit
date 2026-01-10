package com.kuapt.tutor.plan.dto;

public record PlanProgress(long planId, int doneCount, int totalCount, float completionRate, String calculatedAt) {}

