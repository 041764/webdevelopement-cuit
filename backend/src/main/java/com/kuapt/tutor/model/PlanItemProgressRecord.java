package com.kuapt.tutor.model;

public record PlanItemProgressRecord(long id, long planItemId, int percent, String note, long createdByUserId, String createdAt) {}

