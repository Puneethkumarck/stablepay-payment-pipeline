package io.stablepay.api.infrastructure.trino;

public class TrinoAdapterException extends RuntimeException {

  public static final String ERROR_CODE = "STBLPAY-2101";

  public TrinoAdapterException(String message, Throwable cause) {
    super(message, cause);
  }

  public TrinoAdapterException(Throwable cause) {
    super(ERROR_CODE + " Trino query failed", cause);
  }
}
