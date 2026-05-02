package io.stablepay.api.application.web.error;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorCodes {

  public static final String VALIDATION_FAILED = "STBLPAY-1003";
  public static final String INTERNAL_ERROR = "STBLPAY-1999";
  public static final String IDEMPOTENCY_KEY_REQUIRED = "STBLPAY-2001";
  public static final String NOT_FOUND = "STBLPAY-3001";
  public static final String INVALID_CURSOR = "STBLPAY-3010";
  public static final String RATE_LIMITED = "STBLPAY-4001";
}
