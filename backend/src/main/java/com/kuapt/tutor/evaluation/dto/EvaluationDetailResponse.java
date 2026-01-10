package com.kuapt.tutor.evaluation.dto;

import com.kuapt.tutor.model.EvaluationDetailItemRecord;
import com.kuapt.tutor.model.EvaluationRecord;
import java.time.Instant;
import java.util.List;

public record EvaluationDetailResponse(
    long id,
    long evaluatorUserId,
    long evaluateeUserId,
    String term,
    int scoreTotal,
    String comment,
    Instant createdAt,
    List<EvaluationDetailItemRecord> details) {
  public static EvaluationDetailResponse of(EvaluationRecord eval, List<EvaluationDetailItemRecord> details) {
    return new EvaluationDetailResponse(
        eval.id(),
        eval.evaluatorUserId(),
        eval.evaluateeUserId(),
        eval.term(),
        eval.scoreTotal(),
        eval.comment(),
        eval.createdAt(),
        details);
  }
}
