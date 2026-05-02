package io.stablepay.api.application.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.client.ApiError;
import io.stablepay.api.domain.exception.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-02T10:15:30Z");

  private final GlobalExceptionHandler handler =
      new GlobalExceptionHandler(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));

  @Test
  void shouldReturnNotFoundForNotFoundException() {
    // given
    var exception = new NotFoundException("Transaction", "txn-123");
    var expectedBody =
        new ApiError(ErrorCodes.NOT_FOUND, "Transaction not found: txn-123", FIXED_NOW);

    // when
    var actual = handler.handleNotFound(exception);

    // then
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(actual.getBody()).usingRecursiveComparison().isEqualTo(expectedBody);
  }

  @Test
  void shouldReturnBadRequestForIllegalArgument() {
    // given
    var exception = new IllegalArgumentException("Invalid parameter value");
    var expectedBody =
        new ApiError(ErrorCodes.VALIDATION_FAILED, "Invalid parameter value", FIXED_NOW);

    // when
    var actual = handler.handleBadRequest(exception);

    // then
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(actual.getBody()).usingRecursiveComparison().isEqualTo(expectedBody);
  }

  @Test
  void shouldReturnInternalServerErrorForUnhandledException() {
    // given
    var exception = new RuntimeException("Something went wrong");
    var expectedBody = new ApiError(ErrorCodes.INTERNAL_ERROR, "Internal server error", FIXED_NOW);

    // when
    var actual = handler.handleGeneric(exception);

    // then
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(actual.getBody()).usingRecursiveComparison().isEqualTo(expectedBody);
  }
}
