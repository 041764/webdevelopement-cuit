package com.kuapt.tutor.exception;

import com.kuapt.tutor.auth.AuthErrorCode;
import java.util.Map;

public record ErrorResponse(AuthErrorCode code, String message, String requestId, Map<String, Object> details) {
  public static ErrorResponse of(AuthErrorCode code, String message) {
    return new ErrorResponse(code, message, null, null);
  }

  public static ErrorResponse of(AuthErrorCode code, String message, Map<String, Object> details) {
    return new ErrorResponse(code, message, null, details);
  }
}
