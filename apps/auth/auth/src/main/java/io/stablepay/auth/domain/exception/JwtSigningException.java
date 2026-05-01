package io.stablepay.auth.domain.exception;

public class JwtSigningException extends AuthException {

  public static final String ERROR_CODE = "STBLPAY-2001";

  public JwtSigningException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }
}
