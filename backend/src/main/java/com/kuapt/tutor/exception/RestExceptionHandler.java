package com.kuapt.tutor.exception;

import com.kuapt.tutor.auth.AuthErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
    HttpStatus status = switch (ex.code()) {
      case AUTH_INVALID_CREDENTIALS, AUTH_TOKEN_EXPIRED, AUTH_TOKEN_REVOKED -> HttpStatus.UNAUTHORIZED;
      case AUTH_FORBIDDEN -> HttpStatus.FORBIDDEN;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      default -> HttpStatus.UNPROCESSABLE_ENTITY;
    };
    return ResponseEntity.status(status).body(ErrorResponse.of(ex.code(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleInvalid(MethodArgumentNotValidException ex) {
    var fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
    if (fieldError == null) {
      return ResponseEntity.unprocessableEntity().body(ErrorResponse.of(AuthErrorCode.VALIDATION_ERROR, "validation error"));
    }
    return ResponseEntity.unprocessableEntity()
        .body(ErrorResponse.of(AuthErrorCode.VALIDATION_ERROR, "field '" + fieldError.getField() + "' is invalid", Map.of("field", fieldError.getField())));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
    return ResponseEntity.unprocessableEntity().body(ErrorResponse.of(AuthErrorCode.VALIDATION_ERROR, ex.getMessage()));
  }
}
