package com.kuapt.tutor.activity.dto;

import com.kuapt.tutor.model.ActivityRecord;
import java.util.List;

public record PageActivity(int page, int size, long total, List<ActivityRecord> items) {}

