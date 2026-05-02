package io.stablepay.api.application.web.error;

import lombok.Getter;

@Getter
public class IdempotencyKeyConflictException extends RuntimeException {

  private final String errorCode = "STBLPAY-2002";
  private final String idempotencyKey;

  public IdempotencyKeyConflictException(String idempotencyKey) {
    super("Request with idempotency key is already being processed: %s".formatted(idempotencyKey));
    this.idempotencyKey = idempotencyKey;
  }
}
