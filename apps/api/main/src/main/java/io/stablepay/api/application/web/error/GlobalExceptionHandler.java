package io.stablepay.api.application.web.error;

import io.stablepay.api.client.ApiError;
import io.stablepay.api.domain.exception.NotFoundException;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final Clock clock;

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
    log.warn("Resource not found: type={}, id={}", ex.getResourceType(), ex.getResourceId());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(ex.getErrorCode(), ex.getMessage(), clock.instant()));
  }

  @ExceptionHandler(IdempotencyKeyRequiredException.class)
  public ResponseEntity<ApiError> handleIdempotencyKeyRequired(IdempotencyKeyRequiredException ex) {
    log.warn("Idempotency key missing: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(ex.getErrorCode(), ex.getMessage(), clock.instant()));
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  public ResponseEntity<ApiError> handleIdempotencyKeyConflict(IdempotencyKeyConflictException ex) {
    log.warn("Idempotency key conflict: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError(ex.getErrorCode(), ex.getMessage(), clock.instant()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    var message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");
    log.warn("Validation failed: {}", message);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(ErrorCodes.VALIDATION_FAILED, message, clock.instant()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
    log.warn("Bad request: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(ErrorCodes.VALIDATION_FAILED, ex.getMessage(), clock.instant()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError(ErrorCodes.INTERNAL_ERROR, "Internal server error", clock.instant()));
  }
}
