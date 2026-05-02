package io.stablepay.api.infrastructure.opensearch;

public class OpenSearchAdapterException extends RuntimeException {

  public static final String ERROR_CODE = "STBLPAY-2001";

  public OpenSearchAdapterException(String message, Throwable cause) {
    super(message, cause);
  }

  public OpenSearchAdapterException(Throwable cause) {
    super(ERROR_CODE + " OpenSearch query failed", cause);
  }
}
