package io.stablepay.auth.infrastructure.web;

import io.stablepay.auth.client.ApiError;
import java.time.Clock;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private static final String VALIDATION_ERROR_CODE = "STBLPAY-1003";
  private static final String INTERNAL_ERROR_CODE = "STBLPAY-1999";
  private static final String INTERNAL_ERROR_MSG = "Internal server error";

  private final Clock clock;

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    var message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining("; "));
    log.warn("Validation failure: {}", message);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(VALIDATION_ERROR_CODE, message, clock.instant()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError(INTERNAL_ERROR_CODE, INTERNAL_ERROR_MSG, clock.instant()));
  }

  private String formatFieldError(FieldError fe) {
    return fe.getField() + ": " + fe.getDefaultMessage();
  }
}
