package com.kuapt.tutor.model;

public record ActivitySignupRecord(
    long id,
    long activityId,
    long userId,
    SignupStatus status,
    String createdAt,
    String reviewedAt,
    Long reviewedByUserId) {}

