package com.kuapt.tutor.report.dto;

import java.util.List;

public record ReportPlanCompletionResponse(String term, List<Item> items) {
  public record Item(String scope, long doneCount, long totalCount, float completionRate) {}
}
