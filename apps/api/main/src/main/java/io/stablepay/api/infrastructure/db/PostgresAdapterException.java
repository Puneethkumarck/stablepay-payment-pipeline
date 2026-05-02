package io.stablepay.api.infrastructure.db;

public class PostgresAdapterException extends RuntimeException {

  public static final String IDEMPOTENCY_ERROR_CODE = "STBLPAY-2201";
  public static final String OUTBOX_ERROR_CODE = "STBLPAY-2202";

  public PostgresAdapterException(String message, Throwable cause) {
    super(message, cause);
  }

  public static PostgresAdapterException idempotency(Throwable cause) {
    return new PostgresAdapterException(IDEMPOTENCY_ERROR_CODE + " Postgres query failed", cause);
  }

  public static PostgresAdapterException outbox(Throwable cause) {
    return new PostgresAdapterException(OUTBOX_ERROR_CODE + " Postgres query failed", cause);
  }
}
