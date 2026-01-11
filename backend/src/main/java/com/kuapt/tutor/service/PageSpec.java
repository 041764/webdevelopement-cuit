package com.kuapt.tutor.service;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;

public record PageSpec(int page, int size, int offset) {
  public static PageSpec of(int page, int size) {
    if (page < 1) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "page is invalid");
    }
    if (size < 1 || size > 200) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "size is invalid");
    }
    return new PageSpec(page, size, Math.multiplyExact(page - 1, size));
  }
}
