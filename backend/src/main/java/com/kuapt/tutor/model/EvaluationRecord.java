package com.kuapt.tutor.model;

import java.time.Instant;

public record EvaluationRecord(
    long id,
    long evaluatorUserId,
    String evaluatorUserNo,
    String evaluatorUserName,
    long evaluateeUserId,
    String evaluateeUserNo,
    String evaluateeUserName,
    String term,
    int scoreTotal,
    String comment,
    Instant createdAt) {}
