package io.stablepay.api.infrastructure.trino;

/**
 * Thrown when the Trino adapter cannot complete a query. Carries the canonical {@code STBLPAY-2101}
 * error code; the message intentionally avoids bind parameters and row data to keep PII out of
 * logs.
 */
public class TrinoAdapterException extends RuntimeException {

  public static final String ERROR_CODE = "STBLPAY-2101";

  public TrinoAdapterException(String message, Throwable cause) {
    super(message, cause);
  }

  public TrinoAdapterException(Throwable cause) {
    super(ERROR_CODE + " Trino query failed: " + cause.getMessage(), cause);
  }
}
