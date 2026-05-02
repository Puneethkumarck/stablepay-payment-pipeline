package io.stablepay.api.application.web.error;

public class IdempotencyKeyRequiredException extends RuntimeException {

  public IdempotencyKeyRequiredException() {
    super("X-Idempotency-Key header is required");
  }
}
