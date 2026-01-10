package com.kuapt.tutor.activity.dto;

import com.kuapt.tutor.model.ActivitySignupRecord;
import java.util.List;

public record PageActivitySignup(int page, int size, long total, List<ActivitySignupRecord> items) {}

