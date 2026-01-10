package com.kuapt.tutor.model;

public record ActivityRecord(
    long id,
    long classId,
    String term,
    String title,
    String description,
    Integer capacity,
    boolean requiresReview,
    ActivityStatus status,
    long createdByUserId,
    String createdAt) {}

