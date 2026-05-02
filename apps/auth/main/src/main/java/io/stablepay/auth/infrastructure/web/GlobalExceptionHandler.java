package io.stablepay.auth.infrastructure.web;

import io.stablepay.auth.client.ApiError;
import java.time.Clock;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String VALIDATION_ERROR_CODE = "STBLPAY-1003";
  private static final String CLIENT_ERROR_CODE = "STBLPAY-1003";
  private static final String INTERNAL_ERROR_CODE = "STBLPAY-1999";
  private static final String INTERNAL_ERROR_MSG = "Internal server error";

  private final Clock clock;

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiError> handleUnexpected(RuntimeException ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError(INTERNAL_ERROR_CODE, INTERNAL_ERROR_MSG, clock.instant()));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining("; "));
    log.warn("Validation failure: {}", message);
    var body = new ApiError(VALIDATION_ERROR_CODE, message, clock.instant());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(body);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    if (statusCode.is5xxServerError()) {
      log.error("Framework 5xx exception", ex);
    } else {
      log.warn("Framework client exception: {}", ex.getMessage());
    }
    var detail =
        ex instanceof ErrorResponseException ere ? ere.getBody().getDetail() : ex.getMessage();
    var apiError =
        new ApiError(
            statusCode.is5xxServerError() ? INTERNAL_ERROR_CODE : CLIENT_ERROR_CODE,
            statusCode.is5xxServerError() ? INTERNAL_ERROR_MSG : detail,
            clock.instant());
    return ResponseEntity.status(statusCode).headers(headers).body(apiError);
  }

  private String formatFieldError(FieldError fe) {
    return fe.getField() + ": " + fe.getDefaultMessage();
  }
}
