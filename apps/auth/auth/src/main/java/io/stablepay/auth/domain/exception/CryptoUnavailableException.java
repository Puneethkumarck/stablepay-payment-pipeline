package io.stablepay.auth.domain.exception;

public class CryptoUnavailableException extends AuthException {

  public static final String ERROR_CODE = "STBLPAY-2002";

  public CryptoUnavailableException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }
}
