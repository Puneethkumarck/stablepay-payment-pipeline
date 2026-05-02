package io.stablepay.api.application.web.error;

import lombok.Getter;

@Getter
public class IdempotencyKeyRequiredException extends RuntimeException {

  private final String errorCode = "STBLPAY-2001";

  public IdempotencyKeyRequiredException() {
    super("X-Idempotency-Key header is required");
  }
}
