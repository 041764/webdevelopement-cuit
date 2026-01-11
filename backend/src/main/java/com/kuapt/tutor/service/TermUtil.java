package com.kuapt.tutor.service;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;

public final class TermUtil {
  private TermUtil() {}

  public static void validateTerm(String term) {
    if (term == null || !term.matches("\\d{4}-\\d{2}-\\d{2}-[12]")) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "term is invalid");
    }
  }

  public static String normalizeOptionalTerm(String term) {
    if (term == null) {
      return null;
    }
    String t = term.trim();
    if (t.isEmpty()) {
      return null;
    }
    validateTerm(t);
    return t;
  }
}
