package io.stablepay.auth.domain.exception;

public class SigningKeyParseException extends AuthException {

  public static final String ERROR_CODE = "STBLPAY-2004";

  public SigningKeyParseException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }
}
