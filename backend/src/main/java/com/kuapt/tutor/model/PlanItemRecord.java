package com.kuapt.tutor.model;

public record PlanItemRecord(
    long id,
    long planId,
    String title,
    PlanItemStatus status,
    int sortOrder,
    String dueDate,
    String createdAt,
    String updatedAt) {}

