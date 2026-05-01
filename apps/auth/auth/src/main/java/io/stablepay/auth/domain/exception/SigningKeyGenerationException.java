package io.stablepay.auth.domain.exception;

public class SigningKeyGenerationException extends AuthException {

  public static final String ERROR_CODE = "STBLPAY-2003";

  public SigningKeyGenerationException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }
}
