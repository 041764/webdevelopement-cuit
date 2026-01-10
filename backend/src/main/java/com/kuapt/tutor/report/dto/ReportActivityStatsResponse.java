package com.kuapt.tutor.report.dto;

import java.util.List;

public record ReportActivityStatsResponse(String term, List<Item> items) {
  public record Item(long activityId, String title, long appliedCount, long approvedCount) {}
}
