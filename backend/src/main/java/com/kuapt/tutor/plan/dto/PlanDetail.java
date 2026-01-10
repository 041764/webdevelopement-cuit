package com.kuapt.tutor.plan.dto;

import com.kuapt.tutor.model.PlanItemRecord;
import com.kuapt.tutor.model.PlanOwnerType;
import java.util.List;

public record PlanDetail(
    long id,
    PlanOwnerType ownerType,
    Long ownerUserId,
    Long ownerClassId,
    String term,
    String title,
    String createdAt,
    List<PlanItemRecord> items,
    PlanProgress progress) {}

