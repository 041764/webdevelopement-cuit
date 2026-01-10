package com.kuapt.tutor.user.dto;

import java.util.List;

public record UserImportResponse(int created, int updated, int failed, List<Failure> failures) {
  public record Failure(long row, String reason) {}
}
