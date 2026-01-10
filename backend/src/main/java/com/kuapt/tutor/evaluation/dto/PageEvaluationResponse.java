package com.kuapt.tutor.evaluation.dto;

import com.kuapt.tutor.model.EvaluationRecord;
import java.util.List;

public record PageEvaluationResponse(int page, int size, long total, List<EvaluationRecord> items) {}
