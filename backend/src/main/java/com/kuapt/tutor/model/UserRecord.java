package com.kuapt.tutor.model;

public record UserRecord(
    long userId,
    UserType userType,
    String id,
    String name,
    Long collegeId,
    UserStatus status) {}
