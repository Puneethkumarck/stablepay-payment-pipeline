package io.stablepay.auth.infrastructure.web;

import io.stablepay.auth.client.ApiError;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-02T10:15:30Z");

  private final GlobalExceptionHandler handler =
      new GlobalExceptionHandler(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));

  @Test
  void shouldReturn400AndJoinedFieldMessagesOnValidationFailure() throws Exception {
    // given
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "email", "must not be blank"));
    bindingResult.addError(new FieldError("request", "password", "must not be blank"));
    var ex = new MethodArgumentNotValidException(stubMethodParameter(), bindingResult);
    var expected =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                new ApiError(
                    "STBLPAY-1003",
                    "email: must not be blank; password: must not be blank",
                    FIXED_NOW));

    // when
    var actual = handler.handleValidation(ex);

    // then
    Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturn500AndGenericMessageOnUnhandledException() {
    // given
    var ex = new RuntimeException("boom");
    var expected =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError("STBLPAY-1999", "Internal server error", FIXED_NOW));

    // when
    var actual = handler.handleUnexpected(ex);

    // then
    Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  private static MethodParameter stubMethodParameter() throws NoSuchMethodException {
    var method = GlobalExceptionHandlerTest.class.getDeclaredMethod("stubMethodParameter");
    return new MethodParameter(method, -1);
  }
}
