package io.stablepay.api.infrastructure.db;

/**
 * Thrown when a Postgres adapter cannot complete a query. Carries one of the canonical {@code
 * STBLPAY-22XX} error codes; the message intentionally avoids bind parameters and row data to keep
 * PII out of logs.
 */
public class PostgresAdapterException extends RuntimeException {

  public static final String IDEMPOTENCY_ERROR_CODE = "STBLPAY-2201";
  public static final String OUTBOX_ERROR_CODE = "STBLPAY-2202";

  public PostgresAdapterException(String message, Throwable cause) {
    super(message, cause);
  }

  public static PostgresAdapterException idempotency(Throwable cause) {
    return new PostgresAdapterException(
        IDEMPOTENCY_ERROR_CODE + " Postgres query failed: " + cause.getMessage(), cause);
  }

  public static PostgresAdapterException outbox(Throwable cause) {
    return new PostgresAdapterException(
        OUTBOX_ERROR_CODE + " Postgres query failed: " + cause.getMessage(), cause);
  }
}
