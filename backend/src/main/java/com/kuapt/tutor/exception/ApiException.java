package com.kuapt.tutor.exception;

import com.kuapt.tutor.auth.AuthErrorCode;

public class ApiException extends RuntimeException {
  private final AuthErrorCode code;

  public ApiException(AuthErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public AuthErrorCode code() {
    return code;
  }
}
