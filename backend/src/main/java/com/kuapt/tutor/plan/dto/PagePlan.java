package com.kuapt.tutor.plan.dto;

import com.kuapt.tutor.model.PlanRecord;
import java.util.List;

public record PagePlan(int page, int size, long total, List<PlanRecord> items) {}

