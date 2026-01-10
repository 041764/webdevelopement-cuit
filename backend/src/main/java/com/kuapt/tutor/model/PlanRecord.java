package com.kuapt.tutor.model;

public record PlanRecord(long id, PlanOwnerType ownerType, Long ownerUserId, Long ownerClassId, String term, String title, String createdAt) {}

