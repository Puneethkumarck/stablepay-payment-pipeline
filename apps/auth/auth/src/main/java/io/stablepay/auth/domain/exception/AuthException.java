package io.stablepay.auth.domain.exception;

import lombok.Getter;

@Getter
public abstract class AuthException extends RuntimeException {

  private final String errorCode;

  protected AuthException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  protected AuthException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
