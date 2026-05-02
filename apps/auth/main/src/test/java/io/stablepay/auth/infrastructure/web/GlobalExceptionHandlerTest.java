package io.stablepay.auth.infrastructure.web;

import io.stablepay.auth.client.ApiError;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

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
    var expectedBody =
        new ApiError(
            "STBLPAY-1003", "email: must not be blank; password: must not be blank", FIXED_NOW);

    // when
    var actual = handler.handleException(ex, webRequest());

    // then
    Assertions.assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    Assertions.assertThat(actual.getBody()).usingRecursiveComparison().isEqualTo(expectedBody);
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

  @Test
  void shouldReturn400OnMalformedJsonBody() throws Exception {
    // given
    var ex =
        new HttpMessageNotReadableException("malformed", new MockHttpInputMessage(new byte[0]));
    var expectedBody = new ApiError("STBLPAY-1003", "malformed", FIXED_NOW);

    // when
    var actual = handler.handleException(ex, webRequest());

    // then
    Assertions.assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    Assertions.assertThat(actual.getBody()).usingRecursiveComparison().isEqualTo(expectedBody);
  }

  @Test
  void shouldReturn405WhenMethodNotSupported() throws Exception {
    // given
    var ex = new HttpRequestMethodNotSupportedException("GET");

    // when
    var actual = handler.handleException(ex, webRequest());

    // then
    Assertions.assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    Assertions.assertThat(((ApiError) actual.getBody()).errorCode()).isEqualTo("STBLPAY-1003");
  }

  @Test
  void shouldReturn415WhenMediaTypeNotSupported() throws Exception {
    // given
    var ex =
        new HttpMediaTypeNotSupportedException(
            MediaType.TEXT_PLAIN, java.util.List.of(MediaType.APPLICATION_JSON));

    // when
    var actual = handler.handleException(ex, webRequest());

    // then
    Assertions.assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    Assertions.assertThat(((ApiError) actual.getBody()).errorCode()).isEqualTo("STBLPAY-1003");
  }

  private static ServletWebRequest webRequest() {
    var req = new MockHttpServletRequest();
    req.setRequestURI("/api/v1/auth/login");
    return new ServletWebRequest(req);
  }

  private static MethodParameter stubMethodParameter() throws NoSuchMethodException {
    var method = GlobalExceptionHandlerTest.class.getDeclaredMethod("stubMethodParameter");
    return new MethodParameter(method, -1);
  }
}
