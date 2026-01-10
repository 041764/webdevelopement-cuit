package com.kuapt.tutor.model;

import java.time.Instant;

public record EvaluationRecord(
    long id,
    long evaluatorUserId,
    long evaluateeUserId,
    String term,
    int scoreTotal,
    String comment,
    Instant createdAt) {}
