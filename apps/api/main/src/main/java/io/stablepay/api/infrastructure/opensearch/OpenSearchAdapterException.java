package io.stablepay.api.infrastructure.opensearch;

/**
 * Thrown when the OpenSearch adapter cannot complete a query. Carries the canonical {@code
 * STBLPAY-2001} error code; the message intentionally avoids document content to keep PII out of
 * logs.
 */
public class OpenSearchAdapterException extends RuntimeException {

  public static final String ERROR_CODE = "STBLPAY-2001";

  public OpenSearchAdapterException(String message, Throwable cause) {
    super(message, cause);
  }

  public OpenSearchAdapterException(Throwable cause) {
    super(ERROR_CODE + " OpenSearch query failed: " + cause.getMessage(), cause);
  }
}
